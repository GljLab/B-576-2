package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.entity.*;
import com.interview.entity.dto.*;
import com.interview.mapper.*;
import com.interview.service.ScheduleReportService;
import com.interview.service.SchedulingService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleReportServiceImpl implements ScheduleReportService {
    
    @Autowired
    private SchedulingService schedulingService;
    
    @Autowired
    private SchedulingPlanMapper schedulingPlanMapper;
    
    @Autowired
    private SessionAssignmentMapper sessionAssignmentMapper;
    
    @Autowired
    private InterviewSessionMapper interviewSessionMapper;
    
    @Autowired
    private ExaminerWorkloadMapper examinerWorkloadMapper;
    
    @Autowired
    private ExaminerMapper examinerMapper;
    
    @Autowired
    private CandidateMapper candidateMapper;
    
    @Autowired
    private ExamRoomMapper examRoomMapper;
    
    @Autowired
    private PositionMapper positionMapper;
    
    @Autowired
    private InterviewProjectMapper interviewProjectMapper;
    
    @Override
    public SchedulingDashboardDTO getDashboardData(Long projectId) {
        SchedulingDashboardDTO dto = new SchedulingDashboardDTO();
        dto.setProjectId(projectId);
        
        SchedulingPlan executionPlan = schedulingService.getExecutionPlan(projectId);
        List<SessionAssignment> assignments = new ArrayList<>();
        
        if (executionPlan != null) {
            assignments = schedulingService.getAssignmentsByPlanId(executionPlan.getId());
        }
        
        dto.setTotalSessions(assignments.size());
        dto.setPendingSessions((int) assignments.stream().filter(a -> a.getStatus() == 0).count());
        dto.setOngoingSessions((int) assignments.stream().filter(a -> a.getStatus() == 1).count());
        dto.setCompletedSessions((int) assignments.stream().filter(a -> a.getStatus() == 2).count());
        dto.setCancelledSessions((int) assignments.stream().filter(a -> a.getStatus() == 3).count());
        
        List<Candidate> candidates = candidateMapper.selectList(
            new LambdaQueryWrapper<Candidate>().eq(Candidate::getProjectId, projectId));
        dto.setTotalCandidates(candidates.size());
        dto.setCheckedInCandidates((int) candidates.stream().filter(c -> c.getCheckInStatus() != null && c.getCheckInStatus() == 1).count());
        dto.setInterviewedCandidates((int) candidates.stream().filter(c -> c.getInterviewStatus() != null && c.getInterviewStatus() == 2).count());
        
        List<Examiner> examiners = examinerMapper.selectList(
            new LambdaQueryWrapper<Examiner>().eq(Examiner::getProjectId, projectId).eq(Examiner::getStatus, 1));
        dto.setTotalExaminers(examiners.size());
        dto.setAvailableExaminers(examiners.size());
        
        List<ExamRoom> rooms = examRoomMapper.selectList(
            new LambdaQueryWrapper<ExamRoom>().eq(ExamRoom::getProjectId, projectId).eq(ExamRoom::getStatus, 1));
        dto.setTotalRooms(rooms.size());
        dto.setActiveRooms((int) assignments.stream()
            .filter(a -> a.getStatus() != null && a.getStatus() <= 2)
            .map(SessionAssignment::getRoomId)
            .distinct()
            .count());
        
        if (dto.getTotalSessions() > 0) {
            BigDecimal progress = BigDecimal.valueOf(dto.getCompletedSessions())
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(dto.getTotalSessions()), 2, RoundingMode.HALF_UP);
            dto.setOverallProgress(progress);
        } else {
            dto.setOverallProgress(BigDecimal.ZERO);
        }
        
        if (executionPlan != null) {
            dto.setExaminerWorkloads(getExaminerWorkloadDTOs(projectId, executionPlan.getId()));
            dto.setRoomUsages(getRoomUsageDTOs(projectId, executionPlan.getId()));
        }
        
        Map<String, Integer> dailyCount = new LinkedHashMap<>();
        List<InterviewSession> sessions = interviewSessionMapper.selectList(
            new LambdaQueryWrapper<InterviewSession>().eq(InterviewSession::getProjectId, projectId)
                .orderByAsc(InterviewSession::getSessionDate));
        
        for (InterviewSession s : sessions) {
            String dateStr = s.getSessionDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            dailyCount.merge(dateStr, 1, Integer::sum);
        }
        dto.setDailySessionCount(dailyCount);
        
        List<SessionStatusTimelineDTO> timeline = assignments.stream().map(a -> {
            SessionStatusTimelineDTO t = new SessionStatusTimelineDTO();
            t.setAssignmentId(a.getId());
            t.setStatus(a.getStatus());
            t.setEstimatedStartTime(a.getEstimatedStartTime());
            t.setEstimatedEndTime(a.getEstimatedEndTime());
            t.setActualStartTime(a.getActualStartTime());
            t.setActualEndTime(a.getActualEndTime());
            t.setCandidateCount(a.getCandidateCount());
            t.setCompletedCount((int) parseIdList(a.getCandidateIds()).stream()
                .map(id -> candidateMapper.selectById(id))
                .filter(c -> c != null && c.getInterviewStatus() != null && c.getInterviewStatus() == 2)
                .count());
            
            if (a.getSessionId() != null) {
                InterviewSession s = interviewSessionMapper.selectById(a.getSessionId());
                if (s != null) {
                    t.setSessionName(s.getSessionName());
                }
            }
            if (a.getRoomId() != null) {
                ExamRoom r = examRoomMapper.selectById(a.getRoomId());
                if (r != null) {
                    t.setRoomName(r.getRoomName());
                }
            }
            
            return t;
        }).collect(Collectors.toList());
        dto.setTimeline(timeline);
        
        return dto;
    }
    
    @Override
    public ScheduleReportDTO generateReport(Long projectId) {
        ScheduleReportDTO dto = new ScheduleReportDTO();
        dto.setProjectId(projectId);
        
        InterviewProject project = interviewProjectMapper.selectById(projectId);
        if (project != null) {
            dto.setProjectName(project.getProjectName());
        }
        
        SchedulingPlan executionPlan = schedulingService.getExecutionPlan(projectId);
        if (executionPlan == null) {
            List<SchedulingPlan> plans = schedulingService.listPlansByProjectId(projectId);
            if (!plans.isEmpty()) {
                executionPlan = plans.get(0);
            }
        }
        
        if (executionPlan == null) {
            return dto;
        }
        
        List<SessionAssignment> assignments = schedulingService.getAssignmentsByPlanId(executionPlan.getId());
        List<InterviewSession> sessions = interviewSessionMapper.selectList(
            new LambdaQueryWrapper<InterviewSession>().eq(InterviewSession::getProjectId, projectId));
        
        if (!sessions.isEmpty()) {
            dto.setStartDate(sessions.stream().map(InterviewSession::getSessionDate).min(LocalDate::compareTo).orElse(null));
            dto.setEndDate(sessions.stream().map(InterviewSession::getSessionDate).max(LocalDate::compareTo).orElse(null));
        }
        
        dto.setTotalSessions(sessions.size());
        dto.setTotalExaminers(executionPlan.getTotalExaminers());
        dto.setTotalCandidates(executionPlan.getTotalCandidates());
        dto.setTotalRooms(executionPlan.getTotalRooms());
        
        int totalMinutes = assignments.stream()
            .filter(a -> a.getEstimatedStartTime() != null && a.getEstimatedEndTime() != null)
            .mapToInt(a -> (int) Duration.between(a.getEstimatedStartTime(), a.getEstimatedEndTime()).toMinutes())
            .sum();
        dto.setTotalInterviewMinutes(totalMinutes);
        
        if (!assignments.isEmpty()) {
            dto.setAvgCandidatesPerSession(BigDecimal.valueOf(
                (double) executionPlan.getTotalCandidates() / assignments.size())
                .setScale(2, RoundingMode.HALF_UP));
            dto.setAvgExaminersPerSession(BigDecimal.valueOf(
                assignments.stream().mapToInt(a -> parseIdList(a.getExaminerIds()).size()).average().orElse(0))
                .setScale(2, RoundingMode.HALF_UP));
            dto.setAvgSessionDuration(BigDecimal.valueOf(
                (double) totalMinutes / assignments.size())
                .setScale(2, RoundingMode.HALF_UP));
        }
        
        dto.setWorkloadBalanceScore(executionPlan.getWorkloadBalanceScore());
        dto.setRoomUtilizationScore(executionPlan.getRoomUtilizationScore());
        
        dto.setExaminerWorkloads(getExaminerWorkloadDTOs(projectId, executionPlan.getId()));
        dto.setRoomUsages(getRoomUsageDTOs(projectId, executionPlan.getId()));
        
        Map<String, Integer> positionDist = new HashMap<>();
        List<Position> positions = positionMapper.selectList(
            new LambdaQueryWrapper<Position>().eq(Position::getProjectId, projectId));
        for (Position p : positions) {
            long count = candidateMapper.selectCount(
                new LambdaQueryWrapper<Candidate>()
                    .eq(Candidate::getProjectId, projectId)
                    .eq(Candidate::getPositionId, p.getId()));
            positionDist.put(p.getPositionName(), (int) count);
        }
        dto.setPositionDistribution(positionDist);
        
        Map<LocalDate, Integer> dailyDist = new LinkedHashMap<>();
        for (InterviewSession s : sessions) {
            long count = assignments.stream().filter(a -> s.getId().equals(a.getSessionId())).count();
            dailyDist.merge(s.getSessionDate(), (int) count, Integer::sum);
        }
        dto.setDailyDistribution(dailyDist);
        
        List<ScheduleReportDTO.StrategyComparisonDTO> comparisons = new ArrayList<>();
        for (int strategy = 0; strategy <= 2; strategy++) {
            ScheduleReportDTO.StrategyComparisonDTO sc = new ScheduleReportDTO.StrategyComparisonDTO();
            sc.setStrategyName(getStrategyName(strategy));
            comparisons.add(sc);
        }
        dto.setStrategyComparisons(comparisons);
        
        return dto;
    }
    
    @Override
    public ByteArrayOutputStream exportToExcel(Long projectId) {
        try (Workbook workbook = new XSSFWorkbook()) {
            ScheduleReportDTO report = generateReport(projectId);
            
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            Sheet sheet1 = workbook.createSheet("场次安排总览");
            createOverviewSheet(sheet1, report, headerStyle, dataStyle);
            
            Sheet sheet2 = workbook.createSheet("考官分配表");
            createExaminerSheet(sheet2, report, headerStyle, dataStyle);
            
            Sheet sheet3 = workbook.createSheet("考生分组表");
            createCandidateSheet(sheet3, projectId, headerStyle, dataStyle);
            
            Sheet sheet4 = workbook.createSheet("考场时间表");
            createRoomSheet(sheet4, report, headerStyle, dataStyle);
            
            for (int i = 0; i < 4; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (int j = 0; j < 10; j++) {
                    sheet.autoSizeColumn(j);
                }
            }
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;
            
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败: " + e.getMessage(), e);
        }
    }
    
    private void createOverviewSheet(Sheet sheet, ScheduleReportDTO report, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(report.getProjectName() + " - 场次安排总览");
        titleCell.setCellStyle(headerStyle);
        
        rowNum++;
        
        String[][] headers = {
            {"统计项", "数值"},
            {"总场次数", String.valueOf(report.getTotalSessions())},
            {"涉及考官数", String.valueOf(report.getTotalExaminers())},
            {"涉及考生数", String.valueOf(report.getTotalCandidates())},
            {"使用考场数", String.valueOf(report.getTotalRooms())},
            {"总面试时长(分钟)", String.valueOf(report.getTotalInterviewMinutes())},
            {"平均每场考生数", report.getAvgCandidatesPerSession() != null ? report.getAvgCandidatesPerSession().toString() : "-"},
            {"平均每场考官数", report.getAvgExaminersPerSession() != null ? report.getAvgExaminersPerSession().toString() : "-"},
            {"平均场次时长(分钟)", report.getAvgSessionDuration() != null ? report.getAvgSessionDuration().toString() : "-"},
            {"负荷均衡评分", report.getWorkloadBalanceScore() != null ? report.getWorkloadBalanceScore().toString() : "-"},
            {"考场利用率评分", report.getRoomUtilizationScore() != null ? report.getRoomUtilizationScore().toString() : "-"}
        };
        
        for (String[] row : headers) {
            Row dataRow = sheet.createRow(rowNum++);
            for (int i = 0; i < row.length; i++) {
                Cell cell = dataRow.createCell(i);
                cell.setCellValue(row[i]);
                cell.setCellStyle(rowNum == 3 ? headerStyle : dataStyle);
            }
        }
    }
    
    private void createExaminerSheet(Sheet sheet, ScheduleReportDTO report, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"考官姓名", "参与场次数", "评分考生数", "工作时长(分钟)", "负荷评分(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (ExaminerWorkloadDTO wl : report.getExaminerWorkloads()) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(wl.getExaminerName());
            dataRow.createCell(1).setCellValue(wl.getTotalSessions());
            dataRow.createCell(2).setCellValue(wl.getTotalCandidates());
            dataRow.createCell(3).setCellValue(wl.getTotalMinutes());
            dataRow.createCell(4).setCellValue(wl.getWorkloadScore() != null ? wl.getWorkloadScore().doubleValue() : 0);
        }
    }
    
    private void createCandidateSheet(Sheet sheet, Long projectId, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"考生姓名", "准考证号", "报考职位", "面试场次", "考场", "面试顺序"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        SchedulingPlan executionPlan = schedulingService.getExecutionPlan(projectId);
        if (executionPlan != null) {
            List<AssignmentDetailDTO> details = schedulingService.getAssignmentDetailsByPlanId(executionPlan.getId());
            Map<Long, String> sessionNameMap = new HashMap<>();
            Map<Long, String> roomNameMap = new HashMap<>();
            
            for (AssignmentDetailDTO detail : details) {
                String sessionName = detail.getSession() != null ? detail.getSession().getSessionName() : "";
                String roomName = detail.getRoom() != null ? detail.getRoom().getRoomName() : "";
                String positionName = detail.getPosition() != null ? detail.getPosition().getPositionName() : "";
                
                List<Candidate> candidates = detail.getCandidates() != null ? detail.getCandidates() : new ArrayList<>();
                for (int i = 0; i < candidates.size(); i++) {
                    Candidate c = candidates.get(i);
                    Row dataRow = sheet.createRow(rowNum++);
                    dataRow.createCell(0).setCellValue(c.getCandidateName());
                    dataRow.createCell(1).setCellValue(c.getTicketNo() != null ? c.getTicketNo() : "");
                    dataRow.createCell(2).setCellValue(positionName);
                    dataRow.createCell(3).setCellValue(sessionName);
                    dataRow.createCell(4).setCellValue(roomName);
                    dataRow.createCell(5).setCellValue(i + 1);
                }
            }
        }
    }
    
    private void createRoomSheet(Sheet sheet, ScheduleReportDTO report, CellStyle headerStyle, CellStyle dataStyle) {
        int rowNum = 0;
        
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"考场名称", "使用频次", "服务考生数", "使用时长(分钟)", "利用率(%)"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        for (RoomUsageDTO ru : report.getRoomUsages()) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(ru.getRoomName());
            dataRow.createCell(1).setCellValue(ru.getSessionCount());
            dataRow.createCell(2).setCellValue(ru.getCandidateCount());
            dataRow.createCell(3).setCellValue(ru.getTotalMinutes());
            dataRow.createCell(4).setCellValue(ru.getUtilizationRate() != null ? ru.getUtilizationRate().doubleValue() : 0);
        }
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        return style;
    }
    
    @Override
    public ScheduleReportDTO comparePlans(Long planId1, Long planId2) {
        ScheduleReportDTO dto = new ScheduleReportDTO();
        
        SchedulingPlan plan1 = schedulingService.getById(planId1);
        SchedulingPlan plan2 = schedulingService.getById(planId2);
        
        if (plan1 == null || plan2 == null) {
            return dto;
        }
        
        List<ScheduleReportDTO.StrategyComparisonDTO> comparisons = new ArrayList<>();
        
        ScheduleReportDTO.StrategyComparisonDTO c1 = new ScheduleReportDTO.StrategyComparisonDTO();
        c1.setStrategyName("方案1: " + plan1.getPlanName());
        c1.setTotalSessions(plan1.getTotalSessions());
        c1.setEstimatedDuration(plan1.getEstimatedDuration());
        c1.setWorkloadBalanceScore(plan1.getWorkloadBalanceScore());
        c1.setRoomUtilizationScore(plan1.getRoomUtilizationScore());
        c1.setOverallScore(plan1.getOverallScore());
        comparisons.add(c1);
        
        ScheduleReportDTO.StrategyComparisonDTO c2 = new ScheduleReportDTO.StrategyComparisonDTO();
        c2.setStrategyName("方案2: " + plan2.getPlanName());
        c2.setTotalSessions(plan2.getTotalSessions());
        c2.setEstimatedDuration(plan2.getEstimatedDuration());
        c2.setWorkloadBalanceScore(plan2.getWorkloadBalanceScore());
        c2.setRoomUtilizationScore(plan2.getRoomUtilizationScore());
        c2.setOverallScore(plan2.getOverallScore());
        comparisons.add(c2);
        
        dto.setStrategyComparisons(comparisons);
        
        return dto;
    }
    
    private List<ExaminerWorkloadDTO> getExaminerWorkloadDTOs(Long projectId, Long planId) {
        List<ExaminerWorkload> workloads = examinerWorkloadMapper.selectList(
            new LambdaQueryWrapper<ExaminerWorkload>()
                .eq(ExaminerWorkload::getProjectId, projectId)
                .eq(ExaminerWorkload::getPlanId, planId));
        
        int maxMinutes = workloads.stream()
            .mapToInt(w -> w.getTotalMinutes() != null ? w.getTotalMinutes() : 0)
            .max().orElse(1);
        
        return workloads.stream().map(w -> {
            ExaminerWorkloadDTO dto = new ExaminerWorkloadDTO();
            dto.setExaminerId(w.getExaminerId());
            
            Examiner e = examinerMapper.selectById(w.getExaminerId());
            if (e != null) {
                dto.setExaminerName(e.getExaminerName());
            }
            
            dto.setTotalSessions(w.getTotalSessions());
            dto.setTotalCandidates(w.getTotalCandidates());
            dto.setTotalMinutes(w.getTotalMinutes());
            dto.setConsecutiveCount(w.getConsecutiveCount());
            dto.setWorkloadScore(w.getWorkloadScore());
            
            if (maxMinutes > 0 && w.getTotalMinutes() != null) {
                dto.setWorkloadPercentage(BigDecimal.valueOf(
                    (double) w.getTotalMinutes() / maxMinutes * 100)
                    .setScale(2, RoundingMode.HALF_UP));
            }
            
            return dto;
        }).collect(Collectors.toList());
    }
    
    private List<RoomUsageDTO> getRoomUsageDTOs(Long projectId, Long planId) {
        List<SessionAssignment> assignments = sessionAssignmentMapper.selectList(
            new LambdaQueryWrapper<SessionAssignment>().eq(SessionAssignment::getPlanId, planId));
        
        Map<Long, List<SessionAssignment>> byRoom = assignments.stream()
            .filter(a -> a.getRoomId() != null)
            .collect(Collectors.groupingBy(SessionAssignment::getRoomId));
        
        List<RoomUsageDTO> result = new ArrayList<>();
        int maxMinutes = 0;
        
        for (Map.Entry<Long, List<SessionAssignment>> entry : byRoom.entrySet()) {
            RoomUsageDTO dto = new RoomUsageDTO();
            dto.setRoomId(entry.getKey());
            
            ExamRoom room = examRoomMapper.selectById(entry.getKey());
            if (room != null) {
                dto.setRoomName(room.getRoomName());
            }
            
            List<SessionAssignment> roomAssignments = entry.getValue();
            dto.setSessionCount(roomAssignments.size());
            dto.setCandidateCount(roomAssignments.stream()
                .mapToInt(a -> a.getCandidateCount() != null ? a.getCandidateCount() : 0)
                .sum());
            
            int totalMinutes = roomAssignments.stream()
                .filter(a -> a.getEstimatedStartTime() != null && a.getEstimatedEndTime() != null)
                .mapToInt(a -> (int) Duration.between(a.getEstimatedStartTime(), a.getEstimatedEndTime()).toMinutes())
                .sum();
            dto.setTotalMinutes(totalMinutes);
            maxMinutes = Math.max(maxMinutes, totalMinutes);
            
            result.add(dto);
        }
        
        for (RoomUsageDTO dto : result) {
            if (maxMinutes > 0 && dto.getTotalMinutes() != null) {
                dto.setUtilizationRate(BigDecimal.valueOf(
                    (double) dto.getTotalMinutes() / maxMinutes * 100)
                    .setScale(2, RoundingMode.HALF_UP));
            }
        }
        
        return result;
    }
    
    private String getStrategyName(int strategy) {
        switch (strategy) {
            case 0: return "平衡负荷";
            case 1: return "压缩时间";
            case 2: return "优先职位";
            default: return "未知";
        }
    }
    
    private List<Long> parseIdList(String ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return Arrays.stream(ids.split(","))
            .filter(s -> !s.trim().isEmpty())
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }
}

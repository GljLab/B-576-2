package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.entity.*;
import com.interview.entity.dto.*;
import com.interview.mapper.*;
import com.interview.service.SchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulingServiceImpl extends ServiceImpl<SchedulingPlanMapper, SchedulingPlan> implements SchedulingService {
    
    @Autowired
    private SchedulingPlanMapper schedulingPlanMapper;
    
    @Autowired
    private SessionAssignmentMapper sessionAssignmentMapper;
    
    @Autowired
    private InterviewSessionMapper interviewSessionMapper;
    
    @Autowired
    private ExaminerAvailabilityMapper examinerAvailabilityMapper;
    
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
    private ScheduleAdjustmentMapper scheduleAdjustmentMapper;
    
    @Autowired
    private DrawRecordMapper drawRecordMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final int INTERVIEW_TIME_PER_CANDIDATE = 15;
    private static final int STRATEGY_BALANCE = 0;
    private static final int STRATEGY_COMPRESS = 1;
    private static final int STRATEGY_POSITION = 2;
    
    @Override
    @Transactional
    public SchedulingResultDTO generateSchedule(SchedulingRequestDTO dto, Long operatorId, String operatorName) {
        int strategy = dto.getSchedulingStrategy() != null ? dto.getSchedulingStrategy() : STRATEGY_BALANCE;
        return generateScheduleWithStrategy(dto, strategy, operatorId, operatorName);
    }
    
    @Override
    @Transactional
    public SchedulingResultDTO generateScheduleWithStrategy(SchedulingRequestDTO dto, int strategy, Long operatorId, String operatorName) {
        SchedulingResultDTO result = new SchedulingResultDTO();
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        try {
            Long projectId = dto.getProjectId();
            
            List<ConflictInfoDTO> preCheckConflicts = validateConstraints(projectId);
            conflicts.addAll(preCheckConflicts);
            
            boolean hasSevereConflict = conflicts.stream().anyMatch(c -> "HIGH".equals(c.getSeverity()));
            if (hasSevereConflict) {
                result.setSuccess(false);
                result.setMessage("存在严重资源冲突，无法生成调度方案");
                result.setConflicts(conflicts);
                return result;
            }
            
            List<InterviewSession> sessions = interviewSessionMapper.selectList(
                new LambdaQueryWrapper<InterviewSession>()
                    .eq(InterviewSession::getProjectId, projectId)
                    .eq(InterviewSession::getStatus, 0)
                    .orderByAsc(InterviewSession::getSortOrder)
                    .orderByAsc(InterviewSession::getSessionDate)
                    .orderByAsc(InterviewSession::getStartTime));
            
            if (sessions.isEmpty()) {
                ConflictInfoDTO c = new ConflictInfoDTO();
                c.setConflictType("NO_SESSIONS");
                c.setSeverity("HIGH");
                c.setMessage("未配置任何面试场次");
                c.setSuggestion("请先在场次配置中添加面试场次");
                conflicts.add(c);
                result.setSuccess(false);
                result.setMessage("未配置面试场次");
                result.setConflicts(conflicts);
                return result;
            }
            
            List<Examiner> examiners = examinerMapper.selectList(
                new LambdaQueryWrapper<Examiner>()
                    .eq(Examiner::getProjectId, projectId)
                    .eq(Examiner::getStatus, 1));
            
            List<Candidate> candidates = candidateMapper.selectList(
                new LambdaQueryWrapper<Candidate>()
                    .eq(Candidate::getProjectId, projectId)
                    .eq(Candidate::getStatus, 1));
            
            List<ExamRoom> rooms = examRoomMapper.selectList(
                new LambdaQueryWrapper<ExamRoom>()
                    .eq(ExamRoom::getProjectId, projectId)
                    .eq(ExamRoom::getStatus, 1));
            
            Map<Long, List<ExaminerAvailability>> availabilityMap = getAvailabilityMap(projectId);
            
            Map<Long, ExaminerWorkload> currentWorkload = getCurrentWorkloadMap(projectId, null);
            
            List<SessionAssignment> assignments = new ArrayList<>();
            Map<Long, Integer> examinerSessionCount = new HashMap<>();
            Map<Long, Integer> examinerConsecutiveCount = new HashMap<>();
            Map<Long, LocalDateTime> examinerLastSessionEnd = new HashMap<>();
            
            int interviewTime = dto.getInterviewTimePerCandidate() != null ? 
                dto.getInterviewTimePerCandidate() : INTERVIEW_TIME_PER_CANDIDATE;
            
            Map<Long, List<Candidate>> candidatesByPosition = candidates.stream()
                .collect(Collectors.groupingBy(c -> c.getPositionId() != null ? c.getPositionId() : -1L));
            
            int assignmentSortOrder = 0;
            
            for (InterviewSession session : sessions) {
                List<Long> availableRoomIds = parseIdList(session.getRoomIds());
                if (availableRoomIds.isEmpty()) {
                    availableRoomIds = rooms.stream().map(ExamRoom::getId).collect(Collectors.toList());
                }
                
                List<Long> sessionPositionIds = parseIdList(session.getPositionIds());
                boolean isExclusiveStrategy = session.getPositionStrategy() != null && session.getPositionStrategy() == 1;
                
                List<Long> effectivePositionIds = !sessionPositionIds.isEmpty() ? 
                    sessionPositionIds : new ArrayList<>(candidatesByPosition.keySet());
                
                for (Long positionId : effectivePositionIds) {
                    List<Candidate> positionCandidates = candidatesByPosition.getOrDefault(positionId, new ArrayList<>());
                    if (positionCandidates.isEmpty()) continue;
                    
                    int candidatesPerRoom = calculateCandidatesPerRoom(session, interviewTime);
                    
                    List<List<Candidate>> candidateGroups = partitionCandidates(positionCandidates, candidatesPerRoom);
                    
                    for (List<Candidate> group : candidateGroups) {
                        if (group.isEmpty()) continue;
                        
                        for (Long roomId : availableRoomIds) {
                            ExamRoom room = rooms.stream().filter(r -> r.getId().equals(roomId)).findFirst().orElse(null);
                            if (room == null) continue;
                            
                            int requiredExaminers = room.getExaminerCount();
                            
                            List<Examiner> selectedExaminers = selectExaminersForSession(
                                examiners, availabilityMap, currentWorkload,
                                examinerSessionCount, examinerConsecutiveCount, examinerLastSessionEnd,
                                session, requiredExaminers, strategy, roomId
                            );
                            
                            if (selectedExaminers.size() < requiredExaminers) {
                                ConflictInfoDTO c = new ConflictInfoDTO();
                                c.setConflictType("INSUFFICIENT_EXAMINERS");
                                c.setSeverity("MEDIUM");
                                c.setMessage("场次[" + session.getSessionName() + "]考场[" + room.getRoomName() + 
                                    "]考官不足，需要" + requiredExaminers + "人，实际可用" + selectedExaminers.size() + "人");
                                c.setSuggestion("请调整考官可用性或增加备用考官");
                                c.setRelatedId(session.getId());
                                c.setRelatedType("SESSION");
                                conflicts.add(c);
                                continue;
                            }
                            
                            Examiner chiefExaminer = selectChiefExaminer(selectedExaminers, currentWorkload);
                            
                            SessionAssignment assignment = new SessionAssignment();
                            assignment.setSessionId(session.getId());
                            assignment.setRoomId(roomId);
                            assignment.setPositionId(isExclusiveStrategy ? positionId : null);
                            assignment.setExaminerIds(joinIds(selectedExaminers.stream().map(Examiner::getId).collect(Collectors.toList())));
                            assignment.setChiefExaminerId(chiefExaminer.getId());
                            assignment.setCandidateIds(joinIds(group.stream().map(Candidate::getId).collect(Collectors.toList())));
                            assignment.setCandidateCount(group.size());
                            assignment.setSortOrder(assignmentSortOrder++);
                            assignment.setStatus(0);
                            
                            LocalDateTime estimatedStart = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
                            LocalDateTime estimatedEnd = estimatedStart.plusMinutes(
                                (long) group.size() * interviewTime + session.getBreakInterval()
                            );
                            assignment.setEstimatedStartTime(estimatedStart);
                            assignment.setEstimatedEndTime(estimatedEnd);
                            
                            assignments.add(assignment);
                            
                            for (Examiner e : selectedExaminers) {
                                examinerSessionCount.merge(e.getId(), 1, Integer::sum);
                                examinerLastSessionEnd.put(e.getId(), estimatedEnd);
                            }
                            
                            positionCandidates.removeAll(group);
                            
                            break;
                        }
                    }
                }
                
                candidatesByPosition.entrySet().removeIf(e -> e.getValue().isEmpty());
            }
            
            if (!candidatesByPosition.isEmpty()) {
                int remaining = candidatesByPosition.values().stream().mapToInt(List::size).sum();
                ConflictInfoDTO c = new ConflictInfoDTO();
                c.setConflictType("UNASSIGNED_CANDIDATES");
                c.setSeverity("MEDIUM");
                c.setMessage("还有" + remaining + "名考生未分配到场次");
                c.setSuggestion("请增加场次或调整每场容量");
                conflicts.add(c);
            }
            
            SchedulingPlan plan = createPlan(dto, sessions, assignments, conflicts, strategy, operatorId);
            plan.setPlanName(dto.getPlanName() != null ? dto.getPlanName() : 
                "调度方案_" + System.currentTimeMillis());
            
            schedulingPlanMapper.insert(plan);
            
            for (SessionAssignment assignment : assignments) {
                assignment.setPlanId(plan.getId());
                sessionAssignmentMapper.insert(assignment);
            }
            
            saveWorkloadStats(plan.getId(), projectId, assignments, examinerSessionCount);
            
            result.setPlan(plan);
            result.setAssignments(assignments);
            result.setConflicts(conflicts);
            result.setSuccess(conflicts.stream().noneMatch(c -> "HIGH".equals(c.getSeverity())));
            result.setMessage(result.getSuccess() ? "调度方案生成成功" : "调度方案生成，但存在冲突需要处理");
            
            if (dto.getApplyToSystem() != null && dto.getApplyToSystem()) {
                applyPlanToSystem(plan.getId(), operatorId, operatorName);
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("调度算法执行失败: " + e.getMessage());
            ConflictInfoDTO c = new ConflictInfoDTO();
            c.setConflictType("SYSTEM_ERROR");
            c.setSeverity("HIGH");
            c.setMessage("系统错误: " + e.getMessage());
            c.setSuggestion("请联系系统管理员");
            conflicts.add(c);
            result.setConflicts(conflicts);
        }
        
        return result;
    }
    
    private List<Examiner> selectExaminersForSession(
            List<Examiner> allExaminers,
            Map<Long, List<ExaminerAvailability>> availabilityMap,
            Map<Long, ExaminerWorkload> currentWorkload,
            Map<Long, Integer> sessionCount,
            Map<Long, Integer> consecutiveCount,
            Map<Long, LocalDateTime> lastSessionEnd,
            InterviewSession session,
            int requiredCount,
            int strategy,
            Long roomId) {
        
        LocalTime sessionStart = session.getStartTime();
        boolean isMorning = sessionStart.isBefore(LocalTime.of(12, 0));
        
        List<Examiner> availableExaminers = allExaminers.stream().filter(examiner -> {
            List<ExaminerAvailability> availabilities = availabilityMap.get(examiner.getId());
            if (availabilities == null) return false;
            
            ExaminerAvailability availability = availabilities.stream()
                .filter(a -> a.getAvailableDate().equals(session.getSessionDate()))
                .findFirst().orElse(null);
            
            if (availability == null || availability.getTimeSlot() == 3) return false;
            
            int timeSlot = availability.getTimeSlot();
            if (timeSlot == 1 && !isMorning) return false;
            if (timeSlot == 2 && isMorning) return false;
            
            int maxConsecutive = availability.getMaxConsecutiveSessions() != null ? 
                availability.getMaxConsecutiveSessions() : 3;
            int currentConsecutive = consecutiveCount.getOrDefault(examiner.getId(), 0);
            if (currentConsecutive >= maxConsecutive) return false;
            
            LocalDateTime lastEnd = lastSessionEnd.get(examiner.getId());
            if (lastEnd != null) {
                LocalDateTime sessionStartDt = LocalDateTime.of(session.getSessionDate(), session.getStartTime());
                if (Duration.between(lastEnd, sessionStartDt).toMinutes() < session.getBreakInterval()) {
                    return false;
                }
            }
            
            return true;
        }).collect(Collectors.toList());
        
        availableExaminers.sort((e1, e2) -> {
            int score1 = calculateExaminerScore(e1, currentWorkload, sessionCount, consecutiveCount, strategy);
            int score2 = calculateExaminerScore(e2, currentWorkload, sessionCount, consecutiveCount, strategy);
            return Integer.compare(score1, score2);
        });
        
        return availableExaminers.stream().limit(requiredCount).collect(Collectors.toList());
    }
    
    private int calculateExaminerScore(Examiner examiner, 
            Map<Long, ExaminerWorkload> currentWorkload,
            Map<Long, Integer> sessionCount,
            Map<Long, Integer> consecutiveCount,
            int strategy) {
        
        int workload = sessionCount.getOrDefault(examiner.getId(), 0);
        int consecutive = consecutiveCount.getOrDefault(examiner.getId(), 0);
        
        ExaminerWorkload wl = currentWorkload.get(examiner.getId());
        if (wl != null) {
            workload += wl.getTotalSessions() != null ? wl.getTotalSessions() : 0;
        }
        
        int score = workload * 10 + consecutive * 5;
        
        if (strategy == STRATEGY_BALANCE) {
            score = workload * 15 + consecutive * 3;
        } else if (strategy == STRATEGY_COMPRESS) {
            score = consecutive * 2;
        } else if (strategy == STRATEGY_POSITION) {
            score = workload * 5;
        }
        
        return score;
    }
    
    private Examiner selectChiefExaminer(List<Examiner> examiners, Map<Long, ExaminerWorkload> workload) {
        return examiners.stream()
            .max(Comparator.comparingInt(e -> {
                int chiefExp = e.getIsChief() != null && e.getIsChief() == 1 ? 100 : 0;
                ExaminerWorkload wl = workload.get(e.getId());
                int sessions = wl != null && wl.getTotalSessions() != null ? wl.getTotalSessions() : 0;
                return chiefExp + sessions;
            }))
            .orElse(examiners.get(0));
    }
    
    private int calculateCandidatesPerRoom(InterviewSession session, int interviewTime) {
        LocalTime start = session.getStartTime();
        LocalTime end = session.getEndTime();
        int totalMinutes = (int) Duration.between(start, end).toMinutes();
        int capacity = session.getCandidateCapacity() != null ? session.getCandidateCapacity() : 30;
        int maxByTime = totalMinutes / interviewTime;
        return Math.min(capacity, maxByTime);
    }
    
    private List<List<Candidate>> partitionCandidates(List<Candidate> candidates, int groupSize) {
        List<List<Candidate>> groups = new ArrayList<>();
        List<Candidate> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        
        for (int i = 0; i < shuffled.size(); i += groupSize) {
            int end = Math.min(i + groupSize, shuffled.size());
            groups.add(new ArrayList<>(shuffled.subList(i, end)));
        }
        return groups;
    }
    
    private SchedulingPlan createPlan(SchedulingRequestDTO dto, List<InterviewSession> sessions,
            List<SessionAssignment> assignments, List<ConflictInfoDTO> conflicts, int strategy, Long operatorId) {
        
        SchedulingPlan plan = new SchedulingPlan();
        plan.setProjectId(dto.getProjectId());
        plan.setSchedulingStrategy(strategy);
        plan.setTotalSessions(sessions.size());
        plan.setTotalAssignments(assignments.size());
        
        Set<Long> examinerIds = new HashSet<>();
        Set<Long> candidateIds = new HashSet<>();
        Set<Long> roomIds = new HashSet<>();
        int totalMinutes = 0;
        
        for (SessionAssignment a : assignments) {
            parseIdList(a.getExaminerIds()).forEach(examinerIds::add);
            parseIdList(a.getCandidateIds()).forEach(candidateIds::add);
            roomIds.add(a.getRoomId());
            if (a.getEstimatedStartTime() != null && a.getEstimatedEndTime() != null) {
                totalMinutes += Duration.between(a.getEstimatedStartTime(), a.getEstimatedEndTime()).toMinutes();
            }
        }
        
        plan.setTotalExaminers(examinerIds.size());
        plan.setTotalCandidates(candidateIds.size());
        plan.setTotalRooms(roomIds.size());
        plan.setEstimatedDuration(totalMinutes);
        
        BigDecimal workloadBalance = calculateWorkloadBalanceScore(assignments, examinerIds);
        BigDecimal roomUtilization = calculateRoomUtilizationScore(assignments, sessions);
        
        plan.setWorkloadBalanceScore(workloadBalance);
        plan.setRoomUtilizationScore(roomUtilization);
        plan.setOverallScore(workloadBalance.multiply(BigDecimal.valueOf(0.5))
            .add(roomUtilization.multiply(BigDecimal.valueOf(0.5))));
        
        try {
            plan.setConflictInfo(objectMapper.writeValueAsString(conflicts));
        } catch (Exception e) {
            plan.setConflictInfo("[]");
        }
        
        plan.setIsExecution(0);
        plan.setStatus(0);
        plan.setCreateUserId(operatorId);
        
        return plan;
    }
    
    private BigDecimal calculateWorkloadBalanceScore(List<SessionAssignment> assignments, Set<Long> examinerIds) {
        if (examinerIds.isEmpty()) return BigDecimal.ZERO;
        
        Map<Long, Integer> countMap = new HashMap<>();
        for (SessionAssignment a : assignments) {
            parseIdList(a.getExaminerIds()).forEach(id -> countMap.merge(id, 1, Integer::sum));
        }
        
        IntSummaryStatistics stats = countMap.values().stream().mapToInt(Integer::intValue).summaryStatistics();
        if (stats.getMax() == 0) return BigDecimal.valueOf(100);
        
        double ratio = (double) stats.getMin() / stats.getMax();
        return BigDecimal.valueOf(ratio * 100).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateRoomUtilizationScore(List<SessionAssignment> assignments, List<InterviewSession> sessions) {
        if (sessions.isEmpty()) return BigDecimal.ZERO;
        
        int totalSlots = sessions.size() * 3;
        int usedSlots = (int) assignments.stream().map(SessionAssignment::getRoomId).distinct().count() * sessions.size();
        
        double ratio = (double) Math.min(usedSlots, totalSlots) / totalSlots;
        return BigDecimal.valueOf(ratio * 100).setScale(2, RoundingMode.HALF_UP);
    }
    
    private Map<Long, List<ExaminerAvailability>> getAvailabilityMap(Long projectId) {
        List<ExaminerAvailability> all = examinerAvailabilityMapper.selectList(
            new LambdaQueryWrapper<ExaminerAvailability>().eq(ExaminerAvailability::getProjectId, projectId));
        return all.stream().collect(Collectors.groupingBy(ExaminerAvailability::getExaminerId));
    }
    
    private Map<Long, ExaminerWorkload> getCurrentWorkloadMap(Long projectId, Long planId) {
        LambdaQueryWrapper<ExaminerWorkload> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminerWorkload::getProjectId, projectId);
        if (planId != null) {
            wrapper.eq(ExaminerWorkload::getPlanId, planId);
        }
        List<ExaminerWorkload> workloads = examinerWorkloadMapper.selectList(wrapper);
        return workloads.stream().collect(Collectors.toMap(ExaminerWorkload::getExaminerId, w -> w, (a, b) -> a));
    }
    
    private void saveWorkloadStats(Long planId, Long projectId, List<SessionAssignment> assignments, 
            Map<Long, Integer> sessionCount) {
        
        Map<Long, Integer> candidateCount = new HashMap<>();
        Map<Long, Integer> minuteCount = new HashMap<>();
        
        for (SessionAssignment a : assignments) {
            int candidates = a.getCandidateCount() != null ? a.getCandidateCount() : 0;
            int minutes = 0;
            if (a.getEstimatedStartTime() != null && a.getEstimatedEndTime() != null) {
                minutes = (int) Duration.between(a.getEstimatedStartTime(), a.getEstimatedEndTime()).toMinutes();
            }
            
            for (Long examinerId : parseIdList(a.getExaminerIds())) {
                candidateCount.merge(examinerId, candidates, Integer::sum);
                minuteCount.merge(examinerId, minutes, Integer::sum);
            }
        }
        
        for (Long examinerId : sessionCount.keySet()) {
            ExaminerWorkload wl = new ExaminerWorkload();
            wl.setProjectId(projectId);
            wl.setExaminerId(examinerId);
            wl.setPlanId(planId);
            wl.setTotalSessions(sessionCount.get(examinerId));
            wl.setTotalCandidates(candidateCount.getOrDefault(examinerId, 0));
            wl.setTotalMinutes(minuteCount.getOrDefault(examinerId, 0));
            wl.setConsecutiveCount(0);
            
            int maxSessions = sessionCount.values().stream().max(Integer::compare).orElse(1);
            double score = (double) sessionCount.get(examinerId) / maxSessions * 100;
            wl.setWorkloadScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
            
            examinerWorkloadMapper.insert(wl);
        }
    }
    
    @Override
    public List<ConflictInfoDTO> validateConstraints(Long projectId) {
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        List<Examiner> examiners = examinerMapper.selectList(
            new LambdaQueryWrapper<Examiner>().eq(Examiner::getProjectId, projectId).eq(Examiner::getStatus, 1));
        
        List<ExamRoom> rooms = examRoomMapper.selectList(
            new LambdaQueryWrapper<ExamRoom>().eq(ExamRoom::getProjectId, projectId).eq(ExamRoom::getStatus, 1));
        
        List<InterviewSession> sessions = interviewSessionMapper.selectList(
            new LambdaQueryWrapper<InterviewSession>().eq(InterviewSession::getProjectId, projectId));
        
        List<ExaminerAvailability> availabilities = examinerAvailabilityMapper.selectList(
            new LambdaQueryWrapper<ExaminerAvailability>().eq(ExaminerAvailability::getProjectId, projectId));
        
        if (examiners.isEmpty()) {
            ConflictInfoDTO c = new ConflictInfoDTO();
            c.setConflictType("NO_EXAMINERS");
            c.setSeverity("HIGH");
            c.setMessage("项目下没有可用考官");
            c.setSuggestion("请先添加考官");
            conflicts.add(c);
        }
        
        if (rooms.isEmpty()) {
            ConflictInfoDTO c = new ConflictInfoDTO();
            c.setConflictType("NO_ROOMS");
            c.setSeverity("HIGH");
            c.setMessage("项目下没有可用考场");
            c.setSuggestion("请先添加考场");
            conflicts.add(c);
        }
        
        int totalRequiredExaminers = rooms.stream().mapToInt(r -> r.getExaminerCount() != null ? r.getExaminerCount() : 7).sum();
        int availableExaminers = (int) availabilities.stream().filter(a -> a.getTimeSlot() != 3).count();
        
        if (!sessions.isEmpty() && totalRequiredExaminers > examiners.size()) {
            ConflictInfoDTO c = new ConflictInfoDTO();
            c.setConflictType("INSUFFICIENT_TOTAL_EXAMINERS");
            c.setSeverity("HIGH");
            c.setMessage("考官总数不足，需要" + totalRequiredExaminers + "人，现有" + examiners.size() + "人");
            c.setSuggestion("请增加考官数量或减少考场配置的考官人数");
            conflicts.add(c);
        }
        
        Set<Long> examinersWithAvailability = availabilities.stream()
            .map(ExaminerAvailability::getExaminerId).collect(Collectors.toSet());
        
        for (Examiner e : examiners) {
            if (!examinersWithAvailability.contains(e.getId())) {
                ConflictInfoDTO c = new ConflictInfoDTO();
                c.setConflictType("MISSING_AVAILABILITY");
                c.setSeverity("MEDIUM");
                c.setMessage("考官[" + e.getExaminerName() + "]未设置可用性");
                c.setSuggestion("请在考官可用性管理中设置");
                c.setRelatedId(e.getId());
                c.setRelatedType("EXAMINER");
                conflicts.add(c);
            }
        }
        
        for (int i = 0; i < sessions.size(); i++) {
            for (int j = i + 1; j < sessions.size(); j++) {
                InterviewSession s1 = sessions.get(i);
                InterviewSession s2 = sessions.get(j);
                if (s1.getSessionDate().equals(s2.getSessionDate())) {
                    if (!(s1.getEndTime().isBefore(s2.getStartTime()) || s1.getStartTime().isAfter(s2.getEndTime()))) {
                        ConflictInfoDTO c = new ConflictInfoDTO();
                        c.setConflictType("SESSION_OVERLAP");
                        c.setSeverity("MEDIUM");
                        c.setMessage("场次[" + s1.getSessionName() + "]与[" + s2.getSessionName() + "]时间重叠");
                        c.setSuggestion("请调整场次时间避免重叠");
                        c.setRelatedId(s1.getId());
                        c.setRelatedType("SESSION");
                        conflicts.add(c);
                    }
                }
            }
        }
        
        return conflicts;
    }
    
    @Override
    public List<ConflictInfoDTO> validateAssignment(SessionAssignment assignment) {
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        List<Long> examinerIds = parseIdList(assignment.getExaminerIds());
        List<Long> candidateIds = parseIdList(assignment.getCandidateIds());
        
        if (examinerIds.isEmpty()) {
            ConflictInfoDTO c = new ConflictInfoDTO();
            c.setConflictType("NO_EXAMINERS_ASSIGNED");
            c.setSeverity("HIGH");
            c.setMessage("分配未设置考官");
            c.setSuggestion("请添加考官");
            conflicts.add(c);
        }
        
        if (candidateIds.isEmpty()) {
            ConflictInfoDTO c = new ConflictInfoDTO();
            c.setConflictType("NO_CANDIDATES_ASSIGNED");
            c.setSeverity("MEDIUM");
            c.setMessage("分配未设置考生");
            c.setSuggestion("请添加考生");
            conflicts.add(c);
        }
        
        if (assignment.getChiefExaminerId() != null && !examinerIds.contains(assignment.getChiefExaminerId())) {
            ConflictInfoDTO c = new ConflictInfoDTO();
            c.setConflictType("CHIEF_NOT_IN_EXAMINERS");
            c.setSeverity("HIGH");
            c.setMessage("主考官不在分配的考官列表中");
            c.setSuggestion("请重新选择主考官");
            conflicts.add(c);
        }
        
        return conflicts;
    }
    
    @Override
    public List<SchedulingPlan> listPlansByProjectId(Long projectId) {
        return schedulingPlanMapper.selectList(
            new LambdaQueryWrapper<SchedulingPlan>()
                .eq(SchedulingPlan::getProjectId, projectId)
                .orderByDesc(SchedulingPlan::getCreateTime));
    }
    
    @Override
    public SchedulingPlan getExecutionPlan(Long projectId) {
        return schedulingPlanMapper.selectOne(
            new LambdaQueryWrapper<SchedulingPlan>()
                .eq(SchedulingPlan::getProjectId, projectId)
                .eq(SchedulingPlan::getIsExecution, 1));
    }
    
    @Override
    @Transactional
    public boolean setExecutionPlan(Long planId) {
        SchedulingPlan plan = getById(planId);
        if (plan == null) return false;
        
        schedulingPlanMapper.update(null,
            new UpdateWrapper<SchedulingPlan>()
                .lambda()
                .eq(SchedulingPlan::getProjectId, plan.getProjectId())
                .set(SchedulingPlan::getIsExecution, 0));
        
        plan.setIsExecution(1);
        plan.setStatus(1);
        return updateById(plan);
    }
    
    @Override
    public List<SessionAssignment> getAssignmentsByPlanId(Long planId) {
        return sessionAssignmentMapper.selectList(
            new LambdaQueryWrapper<SessionAssignment>()
                .eq(SessionAssignment::getPlanId, planId)
                .orderByAsc(SessionAssignment::getSortOrder));
    }
    
    @Override
    public AssignmentDetailDTO getAssignmentDetail(Long assignmentId) {
        SessionAssignment assignment = sessionAssignmentMapper.selectById(assignmentId);
        if (assignment == null) return null;
        
        AssignmentDetailDTO dto = new AssignmentDetailDTO();
        dto.setAssignment(assignment);
        
        if (assignment.getSessionId() != null) {
            dto.setSession(interviewSessionMapper.selectById(assignment.getSessionId()));
        }
        if (assignment.getRoomId() != null) {
            dto.setRoom(examRoomMapper.selectById(assignment.getRoomId()));
        }
        if (assignment.getPositionId() != null) {
            dto.setPosition(positionMapper.selectById(assignment.getPositionId()));
        }
        if (assignment.getChiefExaminerId() != null) {
            dto.setChiefExaminer(examinerMapper.selectById(assignment.getChiefExaminerId()));
        }
        
        List<Long> examinerIds = parseIdList(assignment.getExaminerIds());
        if (!examinerIds.isEmpty()) {
            dto.setExaminers(examinerMapper.selectBatchIds(examinerIds));
        }
        
        List<Long> candidateIds = parseIdList(assignment.getCandidateIds());
        if (!candidateIds.isEmpty()) {
            dto.setCandidates(candidateMapper.selectBatchIds(candidateIds));
        }
        
        return dto;
    }
    
    @Override
    public List<AssignmentDetailDTO> getAssignmentDetailsByPlanId(Long planId) {
        List<SessionAssignment> assignments = getAssignmentsByPlanId(planId);
        return assignments.stream()
            .map(a -> getAssignmentDetail(a.getId()))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    @Override
    public SchedulingResultDTO compareStrategies(Long projectId) {
        SchedulingResultDTO result = new SchedulingResultDTO();
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        List<SessionAssignment> bestAssignments = null;
        SchedulingPlan bestPlan = null;
        int bestStrategy = STRATEGY_BALANCE;
        BigDecimal bestScore = BigDecimal.ZERO;
        
        for (int strategy = 0; strategy <= 2; strategy++) {
            try {
                SchedulingRequestDTO req = new SchedulingRequestDTO();
                req.setProjectId(projectId);
                req.setSchedulingStrategy(strategy);
                req.setPlanName("策略对比_" + strategy);
                
                SchedulingResultDTO r = generateScheduleWithStrategy(req, strategy, 1L, "系统");
                if (r.getSuccess() && r.getPlan() != null) {
                    BigDecimal score = r.getPlan().getOverallScore();
                    if (score.compareTo(bestScore) > 0) {
                        bestScore = score;
                        bestStrategy = strategy;
                        bestPlan = r.getPlan();
                        bestAssignments = r.getAssignments();
                    }
                    schedulingPlanMapper.deleteById(r.getPlan().getId());
                }
            } catch (Exception e) {
                // 忽略单个策略的错误
            }
        }
        
        result.setPlan(bestPlan);
        result.setAssignments(bestAssignments);
        result.setConflicts(conflicts);
        result.setSuccess(bestPlan != null);
        result.setMessage("最优策略: " + getStrategyName(bestStrategy) + ", 综合评分: " + bestScore);
        
        return result;
    }
    
    private String getStrategyName(int strategy) {
        switch (strategy) {
            case STRATEGY_BALANCE: return "平衡负荷";
            case STRATEGY_COMPRESS: return "压缩时间";
            case STRATEGY_POSITION: return "优先职位";
            default: return "未知";
        }
    }
    
    @Override
    @Transactional
    public boolean applyPlanToSystem(Long planId, Long operatorId, String operatorName) {
        SchedulingPlan plan = getById(planId);
        if (plan == null) return false;
        
        List<SessionAssignment> assignments = getAssignmentsByPlanId(planId);
        
        String batch = "SCHEDULE_" + System.currentTimeMillis();
        
        for (SessionAssignment assignment : assignments) {
            List<Long> examinerIds = parseIdList(assignment.getExaminerIds());
            List<Long> candidateIds = parseIdList(assignment.getCandidateIds());
            
            for (int i = 0; i < examinerIds.size(); i++) {
                Long examinerId = examinerIds.get(i);
                Examiner examiner = examinerMapper.selectById(examinerId);
                if (examiner != null) {
                    examiner.setRoomId(assignment.getRoomId());
                    examiner.setSeatNo(i + 1);
                    examiner.setIsChief(examinerId.equals(assignment.getChiefExaminerId()) ? 1 : 0);
                    examinerMapper.updateById(examiner);
                    
                    DrawRecord record = new DrawRecord();
                    record.setProjectId(plan.getProjectId());
                    record.setDrawType("EXAMINER");
                    record.setDrawBatch(batch);
                    record.setTargetId(examinerId);
                    record.setTargetName(examiner.getExaminerName());
                    record.setOriginalInfo("调度分配");
                    record.setResultInfo("分配至场次ID: " + assignment.getSessionId() + ", 考场: " + assignment.getRoomId());
                    record.setResultOrder(i + 1);
                    record.setRoomId(assignment.getRoomId());
                    record.setDrawTime(LocalDateTime.now());
                    record.setOperatorId(operatorId);
                    record.setOperatorName(operatorName);
                    record.setIsLocked(1);
                    drawRecordMapper.insert(record);
                }
            }
            
            for (int i = 0; i < candidateIds.size(); i++) {
                Long candidateId = candidateIds.get(i);
                Candidate candidate = candidateMapper.selectById(candidateId);
                if (candidate != null) {
                    candidate.setRoomId(assignment.getRoomId());
                    candidate.setInterviewOrder(i + 1);
                    candidateMapper.updateById(candidate);
                    
                    DrawRecord record = new DrawRecord();
                    record.setProjectId(plan.getProjectId());
                    record.setDrawType("CANDIDATE");
                    record.setDrawBatch(batch);
                    record.setTargetId(candidateId);
                    record.setTargetName(candidate.getCandidateName());
                    record.setOriginalInfo("调度分配");
                    record.setResultInfo("分配至场次ID: " + assignment.getSessionId() + ", 考场: " + assignment.getRoomId() + 
                        ", 顺序: " + (i + 1));
                    record.setResultOrder(i + 1);
                    record.setRoomId(assignment.getRoomId());
                    record.setDrawTime(LocalDateTime.now());
                    record.setOperatorId(operatorId);
                    record.setOperatorName(operatorName);
                    record.setIsLocked(1);
                    drawRecordMapper.insert(record);
                }
            }
        }
        
        List<InterviewSession> sessions = interviewSessionMapper.selectList(
            new LambdaQueryWrapper<InterviewSession>().eq(InterviewSession::getProjectId, plan.getProjectId()));
        for (InterviewSession session : sessions) {
            session.setStatus(1);
            interviewSessionMapper.updateById(session);
        }
        
        plan.setStatus(2);
        updateById(plan);
        
        return true;
    }
    
    @Override
    public Map<String, Object> getPlanStatistics(Long planId) {
        Map<String, Object> stats = new HashMap<>();
        
        SchedulingPlan plan = getById(planId);
        if (plan == null) return stats;
        
        List<SessionAssignment> assignments = getAssignmentsByPlanId(planId);
        
        stats.put("totalAssignments", assignments.size());
        stats.put("pendingCount", assignments.stream().filter(a -> a.getStatus() == 0).count());
        stats.put("ongoingCount", assignments.stream().filter(a -> a.getStatus() == 1).count());
        stats.put("completedCount", assignments.stream().filter(a -> a.getStatus() == 2).count());
        stats.put("cancelledCount", assignments.stream().filter(a -> a.getStatus() == 3).count());
        
        Set<Long> examinerIds = new HashSet<>();
        Set<Long> candidateIds = new HashSet<>();
        Set<Long> roomIds = new HashSet<>();
        
        for (SessionAssignment a : assignments) {
            parseIdList(a.getExaminerIds()).forEach(examinerIds::add);
            parseIdList(a.getCandidateIds()).forEach(candidateIds::add);
            roomIds.add(a.getRoomId());
        }
        
        stats.put("examinerCount", examinerIds.size());
        stats.put("candidateCount", candidateIds.size());
        stats.put("roomCount", roomIds.size());
        
        List<ExaminerWorkload> workloads = examinerWorkloadMapper.selectList(
            new LambdaQueryWrapper<ExaminerWorkload>().eq(ExaminerWorkload::getPlanId, planId));
        stats.put("workloads", workloads);
        
        return stats;
    }
    
    private List<Long> parseIdList(String ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return Arrays.stream(ids.split(","))
            .filter(s -> !s.trim().isEmpty())
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }
    
    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "";
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}

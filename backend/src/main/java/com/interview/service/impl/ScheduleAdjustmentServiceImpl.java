package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.entity.*;
import com.interview.entity.dto.AssignmentAdjustDTO;
import com.interview.entity.dto.ConflictInfoDTO;
import com.interview.entity.dto.EmergencyAdjustDTO;
import com.interview.entity.dto.SchedulingResultDTO;
import com.interview.mapper.*;
import com.interview.service.ScheduleAdjustmentService;
import com.interview.service.SchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleAdjustmentServiceImpl extends ServiceImpl<ScheduleAdjustmentMapper, ScheduleAdjustment> 
    implements ScheduleAdjustmentService {
    
    @Autowired
    private ScheduleAdjustmentMapper adjustmentMapper;
    
    @Autowired
    private SessionAssignmentMapper assignmentMapper;
    
    @Autowired
    private ExaminerMapper examinerMapper;
    
    @Autowired
    private CandidateMapper candidateMapper;
    
    @Autowired
    private ExamRoomMapper examRoomMapper;
    
    @Autowired
    private SchedulingService schedulingService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    @Transactional
    public SchedulingResultDTO adjustAssignment(AssignmentAdjustDTO dto) {
        SchedulingResultDTO result = new SchedulingResultDTO();
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        try {
            if ("CANDIDATE_MOVE".equals(dto.getAdjustmentType())) {
                return moveCandidates(dto);
            } else if ("EXAMINER_REPLACE".equals(dto.getAdjustmentType())) {
                return replaceExaminer(dto);
            } else if ("SESSION_ADJUST".equals(dto.getAdjustmentType())) {
                // 其他场次调整逻辑
            }
            
            result.setSuccess(true);
            result.setMessage("调整完成");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("调整失败: " + e.getMessage());
        }
        
        result.setConflicts(conflicts);
        return result;
    }
    
    @Override
    @Transactional
    public SchedulingResultDTO moveCandidates(AssignmentAdjustDTO dto) {
        SchedulingResultDTO result = new SchedulingResultDTO();
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        SessionAssignment fromAssignment = assignmentMapper.selectById(dto.getFromAssignmentId());
        SessionAssignment toAssignment = assignmentMapper.selectById(dto.getToAssignmentId());
        
        if (fromAssignment == null || toAssignment == null) {
            result.setSuccess(false);
            result.setMessage("源分配或目标分配不存在");
            result.setConflicts(conflicts);
            return result;
        }
        
        List<Long> fromCandidateIds = parseIdList(fromAssignment.getCandidateIds());
        List<Long> toCandidateIds = parseIdList(toAssignment.getCandidateIds());
        List<Long> moveIds = dto.getCandidateIds();
        
        Map<String, Object> originalInfo = new HashMap<>();
        originalInfo.put("fromAssignmentId", dto.getFromAssignmentId());
        originalInfo.put("toAssignmentId", dto.getToAssignmentId());
        originalInfo.put("movedCandidateIds", moveIds);
        originalInfo.put("fromCandidatesBefore", new ArrayList<>(fromCandidateIds));
        originalInfo.put("toCandidatesBefore", new ArrayList<>(toCandidateIds));
        
        fromCandidateIds.removeAll(moveIds);
        toCandidateIds.addAll(moveIds);
        
        fromAssignment.setCandidateIds(joinIds(fromCandidateIds));
        fromAssignment.setCandidateCount(fromCandidateIds.size());
        assignmentMapper.updateById(fromAssignment);
        
        toAssignment.setCandidateIds(joinIds(toCandidateIds));
        toAssignment.setCandidateCount(toCandidateIds.size());
        assignmentMapper.updateById(toAssignment);
        
        Map<String, Object> newInfo = new HashMap<>();
        newInfo.put("fromCandidatesAfter", fromCandidateIds);
        newInfo.put("toCandidatesAfter", toCandidateIds);
        
        ScheduleAdjustment adjustment = new ScheduleAdjustment();
        adjustment.setProjectId(schedulingService.getById(dto.getPlanId()).getProjectId());
        adjustment.setPlanId(dto.getPlanId());
        adjustment.setAssignmentId(dto.getFromAssignmentId());
        adjustment.setAdjustmentType("CANDIDATE_MOVE");
        try {
            adjustment.setOriginalInfo(objectMapper.writeValueAsString(originalInfo));
            adjustment.setNewInfo(objectMapper.writeValueAsString(newInfo));
        } catch (Exception e) {
            // ignore
        }
        adjustment.setReason(dto.getReason());
        adjustment.setAffectedCount(moveIds.size());
        adjustment.setOperatorId(dto.getOperatorId());
        adjustment.setOperatorName(dto.getOperatorName());
        adjustmentMapper.insert(adjustment);
        
        conflicts.addAll(schedulingService.validateAssignment(fromAssignment));
        conflicts.addAll(schedulingService.validateAssignment(toAssignment));
        
        result.setSuccess(conflicts.isEmpty());
        result.setMessage(conflicts.isEmpty() ? "考生移动成功" : "考生移动成功，但存在冲突需要注意");
        result.setConflicts(conflicts);
        
        return result;
    }
    
    @Override
    @Transactional
    public SchedulingResultDTO replaceExaminer(AssignmentAdjustDTO dto) {
        SchedulingResultDTO result = new SchedulingResultDTO();
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        if (dto.getAssignmentId() == null && dto.getFromAssignmentId() != null) {
            dto.setAssignmentId(dto.getFromAssignmentId());
        }
        
        SessionAssignment assignment = assignmentMapper.selectById(dto.getAssignmentId());
        if (assignment == null) {
            result.setSuccess(false);
            result.setMessage("分配不存在");
            result.setConflicts(conflicts);
            return result;
        }
        
        Examiner oldExaminer = examinerMapper.selectById(dto.getOldExaminerId());
        Examiner newExaminer = examinerMapper.selectById(dto.getNewExaminerId());
        
        if (oldExaminer == null || newExaminer == null) {
            result.setSuccess(false);
            result.setMessage("考官不存在");
            result.setConflicts(conflicts);
            return result;
        }
        
        List<Long> examinerIds = parseIdList(assignment.getExaminerIds());
        
        Map<String, Object> originalInfo = new HashMap<>();
        originalInfo.put("assignmentId", dto.getAssignmentId());
        originalInfo.put("oldExaminerId", dto.getOldExaminerId());
        originalInfo.put("oldExaminerName", oldExaminer.getExaminerName());
        originalInfo.put("newExaminerId", dto.getNewExaminerId());
        originalInfo.put("newExaminerName", newExaminer.getExaminerName());
        originalInfo.put("examinersBefore", new ArrayList<>(examinerIds));
        
        int index = examinerIds.indexOf(dto.getOldExaminerId());
        if (index >= 0) {
            examinerIds.set(index, dto.getNewExaminerId());
        } else {
            examinerIds.add(dto.getNewExaminerId());
        }
        
        assignment.setExaminerIds(joinIds(examinerIds));
        
        if (dto.getOldExaminerId().equals(assignment.getChiefExaminerId())) {
            assignment.setChiefExaminerId(dto.getNewExaminerId());
        }
        
        assignmentMapper.updateById(assignment);
        
        Map<String, Object> newInfo = new HashMap<>();
        newInfo.put("examinersAfter", examinerIds);
        
        ScheduleAdjustment adjustment = new ScheduleAdjustment();
        SchedulingPlan plan = schedulingService.getById(dto.getPlanId());
        adjustment.setProjectId(plan != null ? plan.getProjectId() : null);
        adjustment.setPlanId(dto.getPlanId());
        adjustment.setAssignmentId(dto.getAssignmentId());
        adjustment.setAdjustmentType("EXAMINER_REPLACE");
        try {
            adjustment.setOriginalInfo(objectMapper.writeValueAsString(originalInfo));
            adjustment.setNewInfo(objectMapper.writeValueAsString(newInfo));
        } catch (Exception e) {
            // ignore
        }
        adjustment.setReason(dto.getReason());
        adjustment.setAffectedCount(assignment.getCandidateCount() != null ? assignment.getCandidateCount() : 0);
        adjustment.setOperatorId(dto.getOperatorId());
        adjustment.setOperatorName(dto.getOperatorName());
        adjustmentMapper.insert(adjustment);
        
        conflicts.addAll(schedulingService.validateAssignment(assignment));
        
        result.setSuccess(conflicts.isEmpty());
        result.setMessage(conflicts.isEmpty() ? "考官替换成功" : "考官替换成功，但存在冲突需要注意");
        result.setConflicts(conflicts);
        
        return result;
    }
    
    @Override
    @Transactional
    public SchedulingResultDTO handleEmergency(EmergencyAdjustDTO dto) {
        SchedulingResultDTO result = new SchedulingResultDTO();
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        try {
            List<Long> affectedIds = dto.getAffectedAssignmentIds();
            if (affectedIds == null || affectedIds.isEmpty()) {
                affectedIds = findAffectedAssignments(dto);
            }
            
            Map<String, Object> originalInfo = new HashMap<>();
            originalInfo.put("emergencyType", dto.getEmergencyType());
            originalInfo.put("examinerId", dto.getExaminerId());
            originalInfo.put("roomId", dto.getRoomId());
            originalInfo.put("candidateId", dto.getCandidateId());
            originalInfo.put("affectedAssignmentIds", affectedIds);
            
            for (Long assignmentId : affectedIds) {
                SessionAssignment assignment = assignmentMapper.selectById(assignmentId);
                if (assignment == null) continue;
                
                if ("EXAMINER_ABSENT".equals(dto.getEmergencyType()) && dto.getExaminerId() != null) {
                    List<Long> examinerIds = parseIdList(assignment.getExaminerIds());
                    if (examinerIds.contains(dto.getExaminerId())) {
                        List<Examiner> availableExaminers = findAvailableExaminers(assignment, dto.getExaminerId());
                        if (!availableExaminers.isEmpty()) {
                            Examiner replacement = availableExaminers.get(0);
                            int idx = examinerIds.indexOf(dto.getExaminerId());
                            examinerIds.set(idx, replacement.getId());
                            assignment.setExaminerIds(joinIds(examinerIds));
                            
                            if (dto.getExaminerId().equals(assignment.getChiefExaminerId())) {
                                assignment.setChiefExaminerId(replacement.getId());
                            }
                            
                            assignmentMapper.updateById(assignment);
                        }
                    }
                } else if ("ROOM_FAULT".equals(dto.getEmergencyType()) && dto.getRoomId() != null) {
                    if (dto.getRoomId().equals(assignment.getRoomId())) {
                        List<ExamRoom> availableRooms = findAvailableRooms(assignment);
                        if (!availableRooms.isEmpty()) {
                            assignment.setRoomId(availableRooms.get(0).getId());
                            assignmentMapper.updateById(assignment);
                        }
                    }
                } else if ("CANDIDATE_ABSENT".equals(dto.getEmergencyType()) && dto.getCandidateId() != null) {
                    List<Long> candidateIds = parseIdList(assignment.getCandidateIds());
                    candidateIds.remove(dto.getCandidateId());
                    assignment.setCandidateIds(joinIds(candidateIds));
                    assignment.setCandidateCount(candidateIds.size());
                    assignmentMapper.updateById(assignment);
                }
            }
            
            ScheduleAdjustment adjustment = new ScheduleAdjustment();
            adjustment.setProjectId(dto.getProjectId());
            adjustment.setPlanId(dto.getPlanId());
            adjustment.setAdjustmentType("EMERGENCY");
            try {
                adjustment.setOriginalInfo(objectMapper.writeValueAsString(originalInfo));
            } catch (Exception e) {
                // ignore
            }
            adjustment.setReason(dto.getReason());
            adjustment.setAffectedCount(affectedIds.size());
            adjustment.setOperatorId(dto.getOperatorId());
            adjustment.setOperatorName(dto.getOperatorName());
            adjustmentMapper.insert(adjustment);
            
            result.setSuccess(true);
            result.setMessage("应急处理完成，影响" + affectedIds.size() + "个分配");
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("应急处理失败: " + e.getMessage());
        }
        
        result.setConflicts(conflicts);
        return result;
    }
    
    private List<Long> findAffectedAssignments(EmergencyAdjustDTO dto) {
        List<Long> result = new ArrayList<>();
        
        LambdaQueryWrapper<SessionAssignment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SessionAssignment::getPlanId, dto.getPlanId());
        wrapper.in(SessionAssignment::getStatus, 0, 1);
        
        List<SessionAssignment> assignments = assignmentMapper.selectList(wrapper);
        
        for (SessionAssignment a : assignments) {
            if (dto.getExaminerId() != null) {
                List<Long> examinerIds = parseIdList(a.getExaminerIds());
                if (examinerIds.contains(dto.getExaminerId())) {
                    result.add(a.getId());
                }
            }
            if (dto.getRoomId() != null && dto.getRoomId().equals(a.getRoomId())) {
                result.add(a.getId());
            }
            if (dto.getCandidateId() != null) {
                List<Long> candidateIds = parseIdList(a.getCandidateIds());
                if (candidateIds.contains(dto.getCandidateId())) {
                    result.add(a.getId());
                }
            }
        }
        
        return result.stream().distinct().collect(Collectors.toList());
    }
    
    private List<Examiner> findAvailableExaminers(SessionAssignment assignment, Long excludeExaminerId) {
        List<Examiner> allExaminers = examinerMapper.selectList(
            new LambdaQueryWrapper<Examiner>()
                .eq(Examiner::getProjectId, 1L)
                .eq(Examiner::getStatus, 1));
        
        List<Long> currentExaminerIds = parseIdList(assignment.getExaminerIds());
        
        return allExaminers.stream()
            .filter(e -> !currentExaminerIds.contains(e.getId()))
            .filter(e -> !e.getId().equals(excludeExaminerId))
            .limit(1)
            .collect(Collectors.toList());
    }
    
    private List<ExamRoom> findAvailableRooms(SessionAssignment assignment) {
        SchedulingPlan plan = schedulingService.getById(assignment.getPlanId());
        Long projectId = plan != null ? plan.getProjectId() : 1L;
        
        List<ExamRoom> allRooms = examRoomMapper.selectList(
            new LambdaQueryWrapper<ExamRoom>()
                .eq(ExamRoom::getProjectId, projectId)
                .eq(ExamRoom::getStatus, 1));
        
        return allRooms.stream()
            .filter(r -> !r.getId().equals(assignment.getRoomId()))
            .limit(1)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ScheduleAdjustment> getAdjustmentHistory(Long projectId, String adjustmentType) {
        LambdaQueryWrapper<ScheduleAdjustment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScheduleAdjustment::getProjectId, projectId);
        if (adjustmentType != null && !adjustmentType.isEmpty()) {
            wrapper.eq(ScheduleAdjustment::getAdjustmentType, adjustmentType);
        }
        wrapper.orderByDesc(ScheduleAdjustment::getCreateTime);
        return adjustmentMapper.selectList(wrapper);
    }
    
    @Override
    public SchedulingResultDTO revalidatePlan(Long planId) {
        SchedulingResultDTO result = new SchedulingResultDTO();
        List<ConflictInfoDTO> conflicts = new ArrayList<>();
        
        SchedulingPlan plan = schedulingService.getById(planId);
        if (plan == null) {
            result.setSuccess(false);
            result.setMessage("方案不存在");
            result.setConflicts(conflicts);
            return result;
        }
        
        List<SessionAssignment> assignments = schedulingService.getAssignmentsByPlanId(planId);
        
        for (SessionAssignment assignment : assignments) {
            conflicts.addAll(schedulingService.validateAssignment(assignment));
        }
        
        result.setSuccess(conflicts.isEmpty());
        result.setMessage(conflicts.isEmpty() ? "方案校验通过，无冲突" : 
            "方案存在" + conflicts.size() + "个冲突需要处理");
        result.setConflicts(conflicts);
        result.setAssignments(assignments);
        result.setPlan(plan);
        
        return result;
    }
    
    @Override
    public int getAffectedCount(AssignmentAdjustDTO dto) {
        if (dto.getCandidateIds() != null && !dto.getCandidateIds().isEmpty()) {
            return dto.getCandidateIds().size();
        }
        
        if (dto.getAssignmentId() != null) {
            SessionAssignment assignment = assignmentMapper.selectById(dto.getAssignmentId());
            if (assignment != null && assignment.getCandidateCount() != null) {
                return assignment.getCandidateCount();
            }
        }
        
        return 0;
    }
    
    @Override
    @Transactional
    public boolean recordAdjustment(ScheduleAdjustment adjustment) {
        return adjustmentMapper.insert(adjustment) > 0;
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

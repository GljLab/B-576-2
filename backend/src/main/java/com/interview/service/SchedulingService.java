package com.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interview.entity.SchedulingPlan;
import com.interview.entity.SessionAssignment;
import com.interview.entity.dto.*;
import java.util.List;
import java.util.Map;

public interface SchedulingService extends IService<SchedulingPlan> {
    
    SchedulingResultDTO generateSchedule(SchedulingRequestDTO dto, Long operatorId, String operatorName);
    
    SchedulingResultDTO generateScheduleWithStrategy(SchedulingRequestDTO dto, int strategy, Long operatorId, String operatorName);
    
    List<ConflictInfoDTO> validateConstraints(Long projectId);
    
    List<ConflictInfoDTO> validateAssignment(SessionAssignment assignment);
    
    List<SchedulingPlan> listPlansByProjectId(Long projectId);
    
    SchedulingPlan getExecutionPlan(Long projectId);
    
    boolean setExecutionPlan(Long planId);
    
    List<SessionAssignment> getAssignmentsByPlanId(Long planId);
    
    AssignmentDetailDTO getAssignmentDetail(Long assignmentId);
    
    List<AssignmentDetailDTO> getAssignmentDetailsByPlanId(Long planId);
    
    SchedulingResultDTO compareStrategies(Long projectId);
    
    boolean applyPlanToSystem(Long planId, Long operatorId, String operatorName);
    
    Map<String, Object> getPlanStatistics(Long planId);
}

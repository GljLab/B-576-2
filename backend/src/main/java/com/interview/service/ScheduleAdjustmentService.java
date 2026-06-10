package com.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interview.entity.ScheduleAdjustment;
import com.interview.entity.dto.AssignmentAdjustDTO;
import com.interview.entity.dto.EmergencyAdjustDTO;
import com.interview.entity.dto.SchedulingResultDTO;
import java.util.List;

public interface ScheduleAdjustmentService extends IService<ScheduleAdjustment> {
    
    SchedulingResultDTO adjustAssignment(AssignmentAdjustDTO dto);
    
    SchedulingResultDTO moveCandidates(AssignmentAdjustDTO dto);
    
    SchedulingResultDTO replaceExaminer(AssignmentAdjustDTO dto);
    
    SchedulingResultDTO handleEmergency(EmergencyAdjustDTO dto);
    
    List<ScheduleAdjustment> getAdjustmentHistory(Long projectId, String adjustmentType);
    
    SchedulingResultDTO revalidatePlan(Long planId);
    
    int getAffectedCount(AssignmentAdjustDTO dto);
    
    boolean recordAdjustment(ScheduleAdjustment adjustment);
}

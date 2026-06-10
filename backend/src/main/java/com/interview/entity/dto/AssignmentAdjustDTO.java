package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class AssignmentAdjustDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long planId;
    
    private Long assignmentId;
    
    private String adjustmentType;
    
    private Long fromAssignmentId;
    
    private Long toAssignmentId;
    
    private Long sourceAssignmentId;
    
    private Long targetAssignmentId;
    
    private List<Long> candidateIds;
    
    private Long oldExaminerId;
    
    private Long newExaminerId;
    
    private String reason;
    
    private Long operatorId;
    
    private String operatorName;
}

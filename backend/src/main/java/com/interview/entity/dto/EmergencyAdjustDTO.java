package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class EmergencyAdjustDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long projectId;
    
    private Long planId;
    
    private String emergencyType;
    
    private Long examinerId;
    
    private Long roomId;
    
    private Long candidateId;
    
    private List<Long> affectedAssignmentIds;
    
    private String reason;
    
    private Long operatorId;
    
    private String operatorName;
}

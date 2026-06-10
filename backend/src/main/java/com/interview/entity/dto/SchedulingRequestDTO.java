package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class SchedulingRequestDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long projectId;
    
    private String planName;
    
    private Integer schedulingStrategy;
    
    private Integer interviewTimePerCandidate;
    
    private Boolean applyToSystem;
}

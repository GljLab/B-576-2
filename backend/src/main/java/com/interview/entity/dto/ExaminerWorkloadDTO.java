package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ExaminerWorkloadDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long examinerId;
    
    private String examinerName;
    
    private Integer totalSessions;
    
    private Integer totalCandidates;
    
    private Integer totalMinutes;
    
    private Integer consecutiveCount;
    
    private BigDecimal workloadScore;
    
    private BigDecimal workloadPercentage;
}

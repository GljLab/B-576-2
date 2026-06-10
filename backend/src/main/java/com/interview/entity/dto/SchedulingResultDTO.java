package com.interview.entity.dto;

import com.interview.entity.SchedulingPlan;
import com.interview.entity.SessionAssignment;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class SchedulingResultDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private SchedulingPlan plan;
    
    private List<SessionAssignment> assignments;
    
    private List<ConflictInfoDTO> conflicts;
    
    private Boolean success;
    
    private String message;
}

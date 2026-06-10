package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class SessionStatusTimelineDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long assignmentId;
    
    private String sessionName;
    
    private String roomName;
    
    private Integer status;
    
    private LocalDateTime estimatedStartTime;
    
    private LocalDateTime estimatedEndTime;
    
    private LocalDateTime actualStartTime;
    
    private LocalDateTime actualEndTime;
    
    private Integer candidateCount;
    
    private Integer completedCount;
}

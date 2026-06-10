package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class SessionConfigDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    
    private Long projectId;
    
    private String sessionName;
    
    private LocalDate sessionDate;
    
    private LocalTime startTime;
    
    private LocalTime endTime;
    
    private Integer breakInterval;
    
    private Integer candidateCapacity;
    
    private List<Long> roomIdList;
    
    private List<Long> positionIdList;
    
    private Integer positionStrategy;
    
    private Integer sortOrder;
    
    private Integer status;
}

package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class RoomUsageDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long roomId;
    
    private String roomName;
    
    private Integer sessionCount;
    
    private Integer candidateCount;
    
    private Integer totalMinutes;
    
    private BigDecimal utilizationRate;
}

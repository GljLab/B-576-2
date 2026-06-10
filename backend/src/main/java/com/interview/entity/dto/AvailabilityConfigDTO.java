package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class AvailabilityConfigDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long projectId;
    
    private Long examinerId;
    
    private List<DateAvailability> dateAvailabilities;
    
    private Integer maxConsecutiveSessions;
    
    private String specialConstraints;
    
    @Data
    public static class DateAvailability implements Serializable {
        private LocalDate date;
        private Integer timeSlot;
        private String remark;
    }
}

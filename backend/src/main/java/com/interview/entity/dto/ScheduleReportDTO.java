package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class ScheduleReportDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long projectId;
    
    private String projectName;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Integer totalSessions;
    
    private Integer totalExaminers;
    
    private Integer totalCandidates;
    
    private Integer totalRooms;
    
    private Integer totalInterviewMinutes;
    
    private BigDecimal avgCandidatesPerSession;
    
    private BigDecimal avgExaminersPerSession;
    
    private BigDecimal avgSessionDuration;
    
    private BigDecimal workloadBalanceScore;
    
    private BigDecimal roomUtilizationScore;
    
    private List<ExaminerWorkloadDTO> examinerWorkloads;
    
    private List<RoomUsageDTO> roomUsages;
    
    private Map<String, Integer> positionDistribution;
    
    private Map<LocalDate, Integer> dailyDistribution;
    
    private List<StrategyComparisonDTO> strategyComparisons;
    
    @Data
    public static class StrategyComparisonDTO implements Serializable {
        private String strategyName;
        private Integer totalSessions;
        private Integer estimatedDuration;
        private BigDecimal workloadBalanceScore;
        private BigDecimal roomUtilizationScore;
        private BigDecimal overallScore;
    }
}

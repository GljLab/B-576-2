package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class SchedulingDashboardDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long projectId;
    
    private Integer totalSessions;
    
    private Integer pendingSessions;
    
    private Integer ongoingSessions;
    
    private Integer completedSessions;
    
    private Integer cancelledSessions;
    
    private Integer totalCandidates;
    
    private Integer checkedInCandidates;
    
    private Integer interviewedCandidates;
    
    private Integer totalExaminers;
    
    private Integer availableExaminers;
    
    private Integer totalRooms;
    
    private Integer activeRooms;
    
    private BigDecimal overallProgress;
    
    private List<ExaminerWorkloadDTO> examinerWorkloads;
    
    private List<RoomUsageDTO> roomUsages;
    
    private Map<String, Integer> dailySessionCount;
    
    private List<SessionStatusTimelineDTO> timeline;
}

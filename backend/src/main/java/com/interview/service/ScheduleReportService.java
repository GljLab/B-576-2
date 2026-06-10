package com.interview.service;

import com.interview.entity.dto.ScheduleReportDTO;
import com.interview.entity.dto.SchedulingDashboardDTO;
import java.io.ByteArrayOutputStream;

public interface ScheduleReportService {
    
    SchedulingDashboardDTO getDashboardData(Long projectId);
    
    ScheduleReportDTO generateReport(Long projectId);
    
    ByteArrayOutputStream exportToExcel(Long projectId);
    
    ScheduleReportDTO comparePlans(Long planId1, Long planId2);
}

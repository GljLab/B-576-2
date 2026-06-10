package com.interview.controller;

import com.interview.common.Result;
import com.interview.entity.dto.ScheduleReportDTO;
import com.interview.entity.dto.SchedulingDashboardDTO;
import com.interview.service.ScheduleReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Tag(name = "报表统计", description = "调度报表与统计分析接口")
@RestController
@RequestMapping("/scheduling/report")
public class ScheduleReportController {
    
    @Autowired
    private ScheduleReportService reportService;
    
    @Operation(summary = "获取调度仪表盘数据")
    @GetMapping("/dashboard")
    public Result<SchedulingDashboardDTO> getDashboardData(@RequestParam Long projectId) {
        SchedulingDashboardDTO dto = reportService.getDashboardData(projectId);
        return Result.success(dto);
    }
    
    @Operation(summary = "生成调度报告")
    @GetMapping("/generate")
    public Result<ScheduleReportDTO> generateReport(@RequestParam Long projectId) {
        ScheduleReportDTO report = reportService.generateReport(projectId);
        return Result.success(report);
    }
    
    @Operation(summary = "导出Excel")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(@RequestParam Long projectId) {
        ByteArrayOutputStream out = reportService.exportToExcel(projectId);
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "面试场次安排_" + timestamp + ".xlsx";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", encodedFilename);
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(out.toByteArray());
    }
    
    @Operation(summary = "对比两个方案")
    @GetMapping("/compare-plans")
    public Result<ScheduleReportDTO> comparePlans(
            @RequestParam Long planId1,
            @RequestParam Long planId2) {
        ScheduleReportDTO report = reportService.comparePlans(planId1, planId2);
        return Result.success(report);
    }
}

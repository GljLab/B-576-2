package com.interview.controller;

import com.interview.common.Result;
import com.interview.entity.ExaminerAvailability;
import com.interview.entity.dto.AvailabilityConfigDTO;
import com.interview.service.ExaminerAvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "考官可用性", description = "考官时间可用性管理接口")
@RestController
@RequestMapping("/scheduling/availability")
public class ExaminerAvailabilityController {
    
    @Autowired
    private ExaminerAvailabilityService availabilityService;
    
    @Operation(summary = "获取项目所有考官可用性")
    @GetMapping("/project")
    public Result<List<ExaminerAvailability>> getProjectAvailability(@RequestParam Long projectId) {
        List<ExaminerAvailability> list = availabilityService.listByProjectId(projectId);
        return Result.success(list);
    }
    
    @Operation(summary = "获取考官可用性日历")
    @GetMapping("/examiner")
    public Result<List<ExaminerAvailability>> getExaminerAvailability(
            @RequestParam Long examinerId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        List<ExaminerAvailability> list = availabilityService.listByExaminerId(examinerId, startDate, endDate);
        return Result.success(list);
    }
    
    @Operation(summary = "获取项目可用性Map")
    @GetMapping("/map")
    public Result<Map<Long, List<ExaminerAvailability>>> getAvailabilityMap(@RequestParam Long projectId) {
        Map<Long, List<ExaminerAvailability>> map = availabilityService.getProjectAvailabilityMap(projectId);
        return Result.success(map);
    }
    
    @Operation(summary = "保存考官可用性")
    @PostMapping("/save")
    public Result<Void> saveAvailability(@RequestBody AvailabilityConfigDTO dto) {
        availabilityService.saveAvailability(dto);
        return Result.success("保存成功", null);
    }
    
    @Operation(summary = "批量保存考官可用性")
    @PostMapping("/batch-save")
    public Result<Void> batchSaveAvailability(
            @RequestParam Long projectId,
            @RequestBody List<AvailabilityConfigDTO> configs) {
        availabilityService.batchSaveAvailability(projectId, configs);
        return Result.success("批量保存成功", null);
    }
    
    @Operation(summary = "初始化默认可用性")
    @PostMapping("/init-default")
    public Result<Void> initDefaultAvailability(@RequestParam Long projectId) {
        availabilityService.initDefaultAvailability(projectId);
        return Result.success("初始化成功", null);
    }
    
    @Operation(summary = "检查可用性冲突")
    @GetMapping("/check-conflict")
    public Result<List<ExaminerAvailability>> checkConflict(
            @RequestParam Long projectId,
            @RequestParam LocalDate date,
            @RequestParam Integer timeSlot) {
        List<ExaminerAvailability> list = availabilityService.checkAvailabilityConflict(projectId, date, timeSlot);
        return Result.success(list);
    }
}

package com.interview.controller;

import com.interview.common.Result;
import com.interview.entity.SchedulingPlan;
import com.interview.entity.SessionAssignment;
import com.interview.entity.dto.*;
import com.interview.service.SchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "智能调度", description = "智能调度算法与方案管理接口")
@RestController
@RequestMapping("/scheduling")
public class SchedulingController {
    
    @Autowired
    private SchedulingService schedulingService;
    
    @Operation(summary = "生成调度方案")
    @PostMapping("/generate")
    public Result<SchedulingResultDTO> generateSchedule(
            @RequestBody SchedulingRequestDTO dto,
            @RequestAttribute(value = "userId", required = false) Long operatorId,
            @RequestAttribute(value = "username", required = false) String operatorName) {
        if (operatorId == null) {
            operatorId = 1L;
            operatorName = "系统管理员";
        }
        SchedulingResultDTO result = schedulingService.generateSchedule(dto, operatorId, operatorName);
        return Result.success(result.getMessage(), result);
    }
    
    @Operation(summary = "按指定策略生成调度方案")
    @PostMapping("/generate-with-strategy")
    public Result<SchedulingResultDTO> generateScheduleWithStrategy(
            @RequestBody SchedulingRequestDTO dto,
            @RequestParam Integer strategy,
            @RequestAttribute(value = "userId", required = false) Long operatorId,
            @RequestAttribute(value = "username", required = false) String operatorName) {
        if (operatorId == null) {
            operatorId = 1L;
            operatorName = "系统管理员";
        }
        SchedulingResultDTO result = schedulingService.generateScheduleWithStrategy(
            dto, strategy, operatorId, operatorName);
        return Result.success(result.getMessage(), result);
    }
    
    @Operation(summary = "校验资源约束")
    @GetMapping("/validate-constraints")
    public Result<List<ConflictInfoDTO>> validateConstraints(@RequestParam Long projectId) {
        List<ConflictInfoDTO> conflicts = schedulingService.validateConstraints(projectId);
        return Result.success(conflicts);
    }
    
    @Operation(summary = "获取项目方案列表")
    @GetMapping("/plans")
    public Result<List<SchedulingPlan>> listPlans(@RequestParam Long projectId) {
        List<SchedulingPlan> plans = schedulingService.listPlansByProjectId(projectId);
        return Result.success(plans);
    }
    
    @Operation(summary = "获取执行方案")
    @GetMapping("/execution-plan")
    public Result<SchedulingPlan> getExecutionPlan(@RequestParam Long projectId) {
        SchedulingPlan plan = schedulingService.getExecutionPlan(projectId);
        return Result.success(plan);
    }
    
    @Operation(summary = "设置为执行方案")
    @PostMapping("/set-execution")
    public Result<Void> setExecutionPlan(@RequestParam Long planId) {
        if (schedulingService.setExecutionPlan(planId)) {
            return Result.success("设置成功", null);
        }
        return Result.error("设置失败");
    }
    
    @Operation(summary = "获取方案分配详情")
    @GetMapping("/assignments")
    public Result<List<SessionAssignment>> getAssignments(@RequestParam Long planId) {
        List<SessionAssignment> assignments = schedulingService.getAssignmentsByPlanId(planId);
        return Result.success(assignments);
    }
    
    @Operation(summary = "获取分配详情（含关联数据）")
    @GetMapping("/assignment-detail")
    public Result<AssignmentDetailDTO> getAssignmentDetail(@RequestParam Long assignmentId) {
        AssignmentDetailDTO detail = schedulingService.getAssignmentDetail(assignmentId);
        return Result.success(detail);
    }
    
    @Operation(summary = "获取方案所有分配详情")
    @GetMapping("/assignment-details")
    public Result<List<AssignmentDetailDTO>> getAssignmentDetails(@RequestParam Long planId) {
        List<AssignmentDetailDTO> details = schedulingService.getAssignmentDetailsByPlanId(planId);
        return Result.success(details);
    }
    
    @Operation(summary = "对比不同策略效果")
    @PostMapping("/compare-strategies")
    public Result<SchedulingResultDTO> compareStrategies(@RequestParam Long projectId) {
        SchedulingResultDTO result = schedulingService.compareStrategies(projectId);
        return Result.success(result);
    }
    
    @Operation(summary = "应用方案到系统")
    @PostMapping("/apply")
    public Result<Void> applyPlanToSystem(
            @RequestParam Long planId,
            @RequestAttribute(value = "userId", required = false) Long operatorId,
            @RequestAttribute(value = "username", required = false) String operatorName) {
        if (operatorId == null) {
            operatorId = 1L;
            operatorName = "系统管理员";
        }
        if (schedulingService.applyPlanToSystem(planId, operatorId, operatorName)) {
            return Result.success("方案已应用到系统", null);
        }
        return Result.error("应用失败");
    }
    
    @Operation(summary = "获取方案统计数据")
    @GetMapping("/plan-statistics")
    public Result<Map<String, Object>> getPlanStatistics(@RequestParam Long planId) {
        Map<String, Object> stats = schedulingService.getPlanStatistics(planId);
        return Result.success(stats);
    }
}

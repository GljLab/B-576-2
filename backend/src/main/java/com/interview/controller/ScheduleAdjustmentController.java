package com.interview.controller;

import com.interview.common.Result;
import com.interview.entity.ScheduleAdjustment;
import com.interview.entity.dto.AssignmentAdjustDTO;
import com.interview.entity.dto.EmergencyAdjustDTO;
import com.interview.entity.dto.SchedulingResultDTO;
import com.interview.service.ScheduleAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "调度调整", description = "手动调整与应急处理接口")
@RestController
@RequestMapping("/scheduling/adjustment")
public class ScheduleAdjustmentController {
    
    @Autowired
    private ScheduleAdjustmentService adjustmentService;
    
    @Operation(summary = "调整分配")
    @PostMapping("/adjust")
    public Result<SchedulingResultDTO> adjustAssignment(
            @RequestBody AssignmentAdjustDTO dto,
            @RequestAttribute(value = "userId", required = false) Long operatorId,
            @RequestAttribute(value = "username", required = false) String operatorName) {
        if (operatorId == null) {
            operatorId = 1L;
            operatorName = "系统管理员";
        }
        dto.setOperatorId(operatorId);
        dto.setOperatorName(operatorName);
        SchedulingResultDTO result = adjustmentService.adjustAssignment(dto);
        return Result.success(result.getMessage(), result);
    }
    
    @Operation(summary = "移动考生")
    @PostMapping("/move-candidates")
    public Result<SchedulingResultDTO> moveCandidates(
            @RequestBody AssignmentAdjustDTO dto,
            @RequestAttribute(value = "userId", required = false) Long operatorId,
            @RequestAttribute(value = "username", required = false) String operatorName) {
        if (operatorId == null) {
            operatorId = 1L;
            operatorName = "系统管理员";
        }
        dto.setOperatorId(operatorId);
        dto.setOperatorName(operatorName);
        dto.setAdjustmentType("CANDIDATE_MOVE");
        SchedulingResultDTO result = adjustmentService.moveCandidates(dto);
        return Result.success(result.getMessage(), result);
    }
    
    @Operation(summary = "替换考官")
    @PostMapping("/replace-examiner")
    public Result<SchedulingResultDTO> replaceExaminer(
            @RequestBody AssignmentAdjustDTO dto,
            @RequestAttribute(value = "userId", required = false) Long operatorId,
            @RequestAttribute(value = "username", required = false) String operatorName) {
        if (operatorId == null) {
            operatorId = 1L;
            operatorName = "系统管理员";
        }
        dto.setOperatorId(operatorId);
        dto.setOperatorName(operatorName);
        dto.setAdjustmentType("EXAMINER_REPLACE");
        SchedulingResultDTO result = adjustmentService.replaceExaminer(dto);
        return Result.success(result.getMessage(), result);
    }
    
    @Operation(summary = "应急处理")
    @PostMapping("/emergency")
    public Result<SchedulingResultDTO> handleEmergency(
            @RequestBody EmergencyAdjustDTO dto,
            @RequestAttribute(value = "userId", required = false) Long operatorId,
            @RequestAttribute(value = "username", required = false) String operatorName) {
        if (operatorId == null) {
            operatorId = 1L;
            operatorName = "系统管理员";
        }
        dto.setOperatorId(operatorId);
        dto.setOperatorName(operatorName);
        SchedulingResultDTO result = adjustmentService.handleEmergency(dto);
        return Result.success(result.getMessage(), result);
    }
    
    @Operation(summary = "获取调整历史")
    @GetMapping("/history")
    public Result<List<ScheduleAdjustment>> getAdjustmentHistory(
            @RequestParam Long projectId,
            @RequestParam(required = false) String adjustmentType) {
        List<ScheduleAdjustment> list = adjustmentService.getAdjustmentHistory(projectId, adjustmentType);
        return Result.success(list);
    }
    
    @Operation(summary = "重新校验方案")
    @PostMapping("/revalidate")
    public Result<SchedulingResultDTO> revalidatePlan(@RequestParam Long planId) {
        SchedulingResultDTO result = adjustmentService.revalidatePlan(planId);
        return Result.success(result.getMessage(), result);
    }
    
    @Operation(summary = "获取影响人数")
    @PostMapping("/affected-count")
    public Result<Integer> getAffectedCount(@RequestBody AssignmentAdjustDTO dto) {
        int count = adjustmentService.getAffectedCount(dto);
        return Result.success(count);
    }
}

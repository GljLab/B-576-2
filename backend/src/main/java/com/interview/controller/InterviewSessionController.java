package com.interview.controller;

import com.interview.common.Result;
import com.interview.entity.InterviewSession;
import com.interview.entity.dto.SessionConfigDTO;
import com.interview.service.InterviewSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "场次管理", description = "面试场次配置相关接口")
@RestController
@RequestMapping("/scheduling/sessions")
public class InterviewSessionController {
    
    @Autowired
    private InterviewSessionService sessionService;
    
    @Operation(summary = "获取项目场次列表")
    @GetMapping("/list")
    public Result<List<InterviewSession>> listSessions(@RequestParam Long projectId) {
        List<InterviewSession> sessions = sessionService.listByProjectId(projectId);
        return Result.success(sessions);
    }
    
    @Operation(summary = "创建场次")
    @PostMapping("/create")
    public Result<InterviewSession> createSession(
            @RequestBody SessionConfigDTO dto,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) userId = 1L;
        InterviewSession session = sessionService.createSession(dto, userId);
        return Result.success("场次创建成功", session);
    }
    
    @Operation(summary = "更新场次")
    @PutMapping("/update")
    public Result<InterviewSession> updateSession(@RequestBody SessionConfigDTO dto) {
        InterviewSession session = sessionService.updateSession(dto);
        if (session == null) {
            return Result.error("场次不存在");
        }
        return Result.success("场次更新成功", session);
    }
    
    @Operation(summary = "删除场次")
    @DeleteMapping("/delete")
    public Result<Void> deleteSession(@RequestParam Long id) {
        if (sessionService.deleteSession(id)) {
            return Result.success("删除成功", null);
        }
        return Result.error("删除失败");
    }
    
    @Operation(summary = "批量创建场次")
    @PostMapping("/batch-create")
    public Result<Void> batchCreateSessions(
            @RequestParam Long projectId,
            @RequestBody List<SessionConfigDTO> sessions,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        if (userId == null) userId = 1L;
        sessionService.batchCreateSessions(projectId, sessions, userId);
        return Result.success("批量创建成功", null);
    }
    
    @Operation(summary = "生成默认场次")
    @PostMapping("/generate-default")
    public Result<List<InterviewSession>> generateDefaultSessions(@RequestParam Long projectId) {
        List<InterviewSession> sessions = sessionService.generateDefaultSessions(projectId);
        return Result.success(sessions);
    }
}

package com.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interview.entity.InterviewSession;
import com.interview.entity.dto.SessionConfigDTO;
import java.util.List;

public interface InterviewSessionService extends IService<InterviewSession> {
    
    List<InterviewSession> listByProjectId(Long projectId);
    
    InterviewSession createSession(SessionConfigDTO dto, Long userId);
    
    InterviewSession updateSession(SessionConfigDTO dto);
    
    boolean deleteSession(Long id);
    
    boolean batchCreateSessions(Long projectId, List<SessionConfigDTO> sessions, Long userId);
    
    List<InterviewSession> generateDefaultSessions(Long projectId);
}

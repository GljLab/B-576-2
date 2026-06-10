package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interview.entity.InterviewProject;
import com.interview.entity.InterviewSession;
import com.interview.entity.dto.SessionConfigDTO;
import com.interview.mapper.InterviewProjectMapper;
import com.interview.mapper.InterviewSessionMapper;
import com.interview.service.InterviewSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InterviewSessionServiceImpl extends ServiceImpl<InterviewSessionMapper, InterviewSession> 
    implements InterviewSessionService {
    
    @Autowired
    private InterviewSessionMapper interviewSessionMapper;
    
    @Autowired
    private InterviewProjectMapper interviewProjectMapper;
    
    @Override
    public List<InterviewSession> listByProjectId(Long projectId) {
        return interviewSessionMapper.selectList(
            new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getProjectId, projectId)
                .orderByAsc(InterviewSession::getSortOrder)
                .orderByAsc(InterviewSession::getSessionDate)
                .orderByAsc(InterviewSession::getStartTime));
    }
    
    @Override
    @Transactional
    public InterviewSession createSession(SessionConfigDTO dto, Long userId) {
        InterviewSession session = convertToEntity(dto);
        session.setCreateUserId(userId);
        session.setStatus(0);
        interviewSessionMapper.insert(session);
        return session;
    }
    
    @Override
    @Transactional
    public InterviewSession updateSession(SessionConfigDTO dto) {
        InterviewSession session = interviewSessionMapper.selectById(dto.getId());
        if (session == null) {
            return null;
        }
        
        InterviewSession updated = convertToEntity(dto);
        updated.setId(dto.getId());
        updated.setCreateUserId(session.getCreateUserId());
        updated.setStatus(session.getStatus());
        
        interviewSessionMapper.updateById(updated);
        return interviewSessionMapper.selectById(dto.getId());
    }
    
    @Override
    @Transactional
    public boolean deleteSession(Long id) {
        return interviewSessionMapper.deleteById(id) > 0;
    }
    
    @Override
    @Transactional
    public boolean batchCreateSessions(Long projectId, List<SessionConfigDTO> sessions, Long userId) {
        for (int i = 0; i < sessions.size(); i++) {
            SessionConfigDTO dto = sessions.get(i);
            dto.setProjectId(projectId);
            if (dto.getSortOrder() == null) {
                dto.setSortOrder(i);
            }
            createSession(dto, userId);
        }
        return true;
    }
    
    @Override
    public List<InterviewSession> generateDefaultSessions(Long projectId) {
        List<InterviewSession> sessions = new ArrayList<>();
        InterviewProject project = interviewProjectMapper.selectById(projectId);
        if (project == null) {
            return sessions;
        }
        
        LocalDate startDate = project.getInterviewDate();
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        
        LocalTime morningStart = LocalTime.of(8, 30);
        LocalTime morningEnd = LocalTime.of(12, 0);
        LocalTime afternoonStart = LocalTime.of(13, 30);
        LocalTime afternoonEnd = LocalTime.of(17, 30);
        
        for (int day = 0; day < 3; day++) {
            LocalDate sessionDate = startDate.plusDays(day);
            
            InterviewSession morning = new InterviewSession();
            morning.setProjectId(projectId);
            morning.setSessionName("第" + (day + 1) + "天上午场");
            morning.setSessionDate(sessionDate);
            morning.setStartTime(morningStart);
            morning.setEndTime(morningEnd);
            morning.setBreakInterval(15);
            morning.setCandidateCapacity(30);
            morning.setSortOrder(day * 2);
            morning.setStatus(0);
            sessions.add(morning);
            
            InterviewSession afternoon = new InterviewSession();
            afternoon.setProjectId(projectId);
            afternoon.setSessionName("第" + (day + 1) + "天下午场");
            afternoon.setSessionDate(sessionDate);
            afternoon.setStartTime(afternoonStart);
            afternoon.setEndTime(afternoonEnd);
            afternoon.setBreakInterval(15);
            afternoon.setCandidateCapacity(30);
            afternoon.setSortOrder(day * 2 + 1);
            afternoon.setStatus(0);
            sessions.add(afternoon);
        }
        
        return sessions;
    }
    
    private InterviewSession convertToEntity(SessionConfigDTO dto) {
        InterviewSession session = new InterviewSession();
        session.setProjectId(dto.getProjectId());
        session.setSessionName(dto.getSessionName());
        session.setSessionDate(dto.getSessionDate());
        session.setStartTime(dto.getStartTime());
        session.setEndTime(dto.getEndTime());
        session.setBreakInterval(dto.getBreakInterval() != null ? dto.getBreakInterval() : 15);
        session.setCandidateCapacity(dto.getCandidateCapacity() != null ? dto.getCandidateCapacity() : 30);
        session.setRoomIds(dto.getRoomIdList() != null ? joinIds(dto.getRoomIdList()) : null);
        session.setPositionIds(dto.getPositionIdList() != null ? joinIds(dto.getPositionIdList()) : null);
        session.setPositionStrategy(dto.getPositionStrategy() != null ? dto.getPositionStrategy() : 0);
        session.setSortOrder(dto.getSortOrder());
        session.setStatus(dto.getStatus());
        return session;
    }
    
    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return "";
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}

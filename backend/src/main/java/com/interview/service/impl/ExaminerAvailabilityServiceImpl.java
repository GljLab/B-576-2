package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interview.entity.Examiner;
import com.interview.entity.ExaminerAvailability;
import com.interview.entity.InterviewSession;
import com.interview.entity.dto.AvailabilityConfigDTO;
import com.interview.mapper.ExaminerAvailabilityMapper;
import com.interview.mapper.ExaminerMapper;
import com.interview.mapper.InterviewSessionMapper;
import com.interview.service.ExaminerAvailabilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExaminerAvailabilityServiceImpl extends ServiceImpl<ExaminerAvailabilityMapper, ExaminerAvailability> 
    implements ExaminerAvailabilityService {
    
    @Autowired
    private ExaminerAvailabilityMapper availabilityMapper;
    
    @Autowired
    private ExaminerMapper examinerMapper;
    
    @Autowired
    private InterviewSessionMapper interviewSessionMapper;
    
    @Override
    public List<ExaminerAvailability> listByProjectId(Long projectId) {
        return availabilityMapper.selectList(
            new LambdaQueryWrapper<ExaminerAvailability>()
                .eq(ExaminerAvailability::getProjectId, projectId)
                .orderByAsc(ExaminerAvailability::getAvailableDate));
    }
    
    @Override
    public List<ExaminerAvailability> listByExaminerId(Long examinerId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ExaminerAvailability> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExaminerAvailability::getExaminerId, examinerId);
        if (startDate != null) {
            wrapper.ge(ExaminerAvailability::getAvailableDate, startDate);
        }
        if (endDate != null) {
            wrapper.le(ExaminerAvailability::getAvailableDate, endDate);
        }
        wrapper.orderByAsc(ExaminerAvailability::getAvailableDate);
        return availabilityMapper.selectList(wrapper);
    }
    
    @Override
    public Map<Long, List<ExaminerAvailability>> getProjectAvailabilityMap(Long projectId) {
        List<ExaminerAvailability> all = listByProjectId(projectId);
        return all.stream().collect(Collectors.groupingBy(ExaminerAvailability::getExaminerId));
    }
    
    @Override
    @Transactional
    public boolean saveAvailability(AvailabilityConfigDTO dto) {
        List<ExaminerAvailability> existing = availabilityMapper.selectList(
            new LambdaQueryWrapper<ExaminerAvailability>()
                .eq(ExaminerAvailability::getProjectId, dto.getProjectId())
                .eq(ExaminerAvailability::getExaminerId, dto.getExaminerId()));
        
        Map<LocalDate, ExaminerAvailability> existingMap = existing.stream()
            .collect(Collectors.toMap(ExaminerAvailability::getAvailableDate, a -> a));
        
        for (AvailabilityConfigDTO.DateAvailability da : dto.getDateAvailabilities()) {
            ExaminerAvailability availability = existingMap.get(da.getDate());
            
            if (availability == null) {
                availability = new ExaminerAvailability();
                availability.setProjectId(dto.getProjectId());
                availability.setExaminerId(dto.getExaminerId());
                availability.setAvailableDate(da.getDate());
                availability.setTimeSlot(da.getTimeSlot());
                availability.setMaxConsecutiveSessions(dto.getMaxConsecutiveSessions() != null ? dto.getMaxConsecutiveSessions() : 3);
                availability.setSpecialConstraints(dto.getSpecialConstraints());
                availability.setRemark(da.getRemark());
                availabilityMapper.insert(availability);
            } else {
                availability.setTimeSlot(da.getTimeSlot());
                if (dto.getMaxConsecutiveSessions() != null) {
                    availability.setMaxConsecutiveSessions(dto.getMaxConsecutiveSessions());
                }
                if (dto.getSpecialConstraints() != null) {
                    availability.setSpecialConstraints(dto.getSpecialConstraints());
                }
                availability.setRemark(da.getRemark());
                availabilityMapper.updateById(availability);
            }
        }
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean batchSaveAvailability(Long projectId, List<AvailabilityConfigDTO> configs) {
        for (AvailabilityConfigDTO config : configs) {
            config.setProjectId(projectId);
            saveAvailability(config);
        }
        return true;
    }
    
    @Override
    @Transactional
    public boolean updateWorkloadStats(Long examinerId, int sessions, int candidates) {
        List<ExaminerAvailability> availabilities = availabilityMapper.selectList(
            new LambdaQueryWrapper<ExaminerAvailability>()
                .eq(ExaminerAvailability::getExaminerId, examinerId));
        
        for (ExaminerAvailability a : availabilities) {
            a.setTotalSessions((a.getTotalSessions() != null ? a.getTotalSessions() : 0) + sessions);
            a.setTotalCandidates((a.getTotalCandidates() != null ? a.getTotalCandidates() : 0) + candidates);
            availabilityMapper.updateById(a);
        }
        
        return true;
    }
    
    @Override
    public List<ExaminerAvailability> checkAvailabilityConflict(Long projectId, LocalDate date, Integer timeSlot) {
        return availabilityMapper.selectList(
            new LambdaQueryWrapper<ExaminerAvailability>()
                .eq(ExaminerAvailability::getProjectId, projectId)
                .eq(ExaminerAvailability::getAvailableDate, date)
                .eq(ExaminerAvailability::getTimeSlot, timeSlot));
    }
    
    @Override
    @Transactional
    public void initDefaultAvailability(Long projectId) {
        List<Examiner> examiners = examinerMapper.selectList(
            new LambdaQueryWrapper<Examiner>()
                .eq(Examiner::getProjectId, projectId)
                .eq(Examiner::getStatus, 1));
        
        List<InterviewProjectSession> sessions = getProjectSessions(projectId);
        List<LocalDate> dates = sessions.stream()
            .map(s -> s.sessionDate)
            .distinct()
            .collect(Collectors.toList());
        
        for (Examiner examiner : examiners) {
            for (LocalDate date : dates) {
                LambdaQueryWrapper<ExaminerAvailability> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ExaminerAvailability::getExaminerId, examiner.getId())
                    .eq(ExaminerAvailability::getAvailableDate, date);
                
                if (availabilityMapper.selectCount(wrapper) == 0) {
                    ExaminerAvailability availability = new ExaminerAvailability();
                    availability.setProjectId(projectId);
                    availability.setExaminerId(examiner.getId());
                    availability.setAvailableDate(date);
                    availability.setTimeSlot(0);
                    availability.setMaxConsecutiveSessions(3);
                    availabilityMapper.insert(availability);
                }
            }
        }
    }
    
    private List<InterviewProjectSession> getProjectSessions(Long projectId) {
        List<InterviewProjectSession> sessions = interviewSessionMapper.selectList(
            new LambdaQueryWrapper<InterviewSession>()
                .eq(InterviewSession::getProjectId, projectId)
                .select(InterviewSession::getSessionDate))
            .stream()
            .map(s -> new InterviewProjectSession(s.getSessionDate()))
            .collect(Collectors.toList());
        
        if (sessions.isEmpty()) {
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 3; i++) {
                sessions.add(new InterviewProjectSession(today.plusDays(i)));
            }
        }
        
        return sessions;
    }
    
    private static class InterviewProjectSession {
        LocalDate sessionDate;
        
        InterviewProjectSession(LocalDate sessionDate) {
            this.sessionDate = sessionDate;
        }
    }
}

package com.interview.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.interview.entity.ExaminerAvailability;
import com.interview.entity.dto.AvailabilityConfigDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ExaminerAvailabilityService extends IService<ExaminerAvailability> {
    
    List<ExaminerAvailability> listByProjectId(Long projectId);
    
    List<ExaminerAvailability> listByExaminerId(Long examinerId, LocalDate startDate, LocalDate endDate);
    
    Map<Long, List<ExaminerAvailability>> getProjectAvailabilityMap(Long projectId);
    
    boolean saveAvailability(AvailabilityConfigDTO dto);
    
    boolean batchSaveAvailability(Long projectId, List<AvailabilityConfigDTO> configs);
    
    boolean updateWorkloadStats(Long examinerId, int sessions, int candidates);
    
    List<ExaminerAvailability> checkAvailabilityConflict(Long projectId, LocalDate date, Integer timeSlot);
    
    void initDefaultAvailability(Long projectId);
}

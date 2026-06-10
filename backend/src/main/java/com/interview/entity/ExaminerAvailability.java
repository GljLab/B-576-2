package com.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("examiner_availability")
public class ExaminerAvailability implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private Long examinerId;
    
    private LocalDate availableDate;
    
    private Integer timeSlot;
    
    private Integer totalSessions;
    
    private Integer totalCandidates;
    
    private Integer maxConsecutiveSessions;
    
    private String specialConstraints;
    
    private String remark;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

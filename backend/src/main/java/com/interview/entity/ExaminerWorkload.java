package com.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("examiner_workload")
public class ExaminerWorkload implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private Long examinerId;
    
    private Long planId;
    
    private Integer totalSessions;
    
    private Integer totalCandidates;
    
    private Integer totalMinutes;
    
    private Integer consecutiveCount;
    
    private BigDecimal workloadScore;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

package com.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("schedule_adjustment")
public class ScheduleAdjustment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private Long planId;
    
    private Long assignmentId;
    
    private String adjustmentType;
    
    private String originalInfo;
    
    private String newInfo;
    
    private String reason;
    
    private Integer affectedCount;
    
    private Long operatorId;
    
    private String operatorName;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

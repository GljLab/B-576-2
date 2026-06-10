package com.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("scheduling_plan")
public class SchedulingPlan implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private String planName;
    
    private String planVersion;
    
    private Integer schedulingStrategy;
    
    private Integer totalSessions;
    
    private Integer totalAssignments;
    
    private Integer totalExaminers;
    
    private Integer totalCandidates;
    
    private Integer totalRooms;
    
    private Integer estimatedDuration;
    
    private BigDecimal workloadBalanceScore;
    
    private BigDecimal roomUtilizationScore;
    
    private BigDecimal overallScore;
    
    private String conflictInfo;
    
    private Integer isExecution;
    
    private Integer status;
    
    private String remark;
    
    private Long createUserId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

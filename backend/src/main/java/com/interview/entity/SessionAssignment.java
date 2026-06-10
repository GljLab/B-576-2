package com.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("session_assignment")
public class SessionAssignment implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long planId;
    
    private Long sessionId;
    
    private Long roomId;
    
    private Long positionId;
    
    private String examinerIds;
    
    private Long chiefExaminerId;
    
    private String candidateIds;
    
    private Integer candidateCount;
    
    private LocalDateTime estimatedStartTime;
    
    private LocalDateTime estimatedEndTime;
    
    private LocalDateTime actualStartTime;
    
    private LocalDateTime actualEndTime;
    
    private Integer sortOrder;
    
    private Integer status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

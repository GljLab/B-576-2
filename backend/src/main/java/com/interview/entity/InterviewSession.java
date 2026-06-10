package com.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("interview_session")
public class InterviewSession implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long projectId;
    
    private String sessionName;
    
    private LocalDate sessionDate;
    
    private LocalTime startTime;
    
    private LocalTime endTime;
    
    private Integer breakInterval;
    
    private Integer candidateCapacity;
    
    private String roomIds;
    
    private String positionIds;
    
    private Integer positionStrategy;
    
    private Integer sortOrder;
    
    private Integer status;
    
    private Long createUserId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

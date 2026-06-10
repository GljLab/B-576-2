package com.interview.entity.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class ConflictInfoDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String conflictType;
    
    private String severity;
    
    private String message;
    
    private String suggestion;
    
    private Long relatedId;
    
    private String relatedType;
}

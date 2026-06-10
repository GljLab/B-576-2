package com.interview.entity.dto;

import com.interview.entity.SessionAssignment;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import com.interview.entity.Candidate;
import com.interview.entity.ExamRoom;
import com.interview.entity.Examiner;
import com.interview.entity.InterviewSession;
import com.interview.entity.Position;

@Data
public class AssignmentDetailDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private SessionAssignment assignment;
    
    private InterviewSession session;
    
    private ExamRoom room;
    
    private Position position;
    
    private List<Examiner> examiners;
    
    private Examiner chiefExaminer;
    
    private List<Candidate> candidates;
}

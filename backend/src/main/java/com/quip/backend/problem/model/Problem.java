package com.quip.backend.problem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {
    private long problemId;
    private String question;
    private List<String> choices;
    private int correctAnswerIndex;
    private int numAsked;
    private int numCorrect;
    private String mediaUrl;
    private long contributorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
package com.quip.backend.problem.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDto {
    private long problemId;
    private String question;
    private List<String> choices;
    private int correctAnswerIndex;
    private int numAsked;
    private int numCorrect;
    private String mediaUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void shuffleChoices() {
        Collections.shuffle(choices);
    }
}
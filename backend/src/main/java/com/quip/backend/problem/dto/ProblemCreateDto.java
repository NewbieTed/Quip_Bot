package com.quip.backend.problem.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemCreateDto {
    private String question;
    private List<String> choices;
    private int correctAnswerIndex;
    private int numAsked;
    private int numCorrect;
    private String mediaUrl;
}

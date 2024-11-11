package com.quip.backend.problem.dto;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor  // Generates a no-args constructor
@AllArgsConstructor // Generates an all-args constructor
public class ProblemDto {
    private long problemId;
    private String question;
    private List<String> choices;
    private int numAsked;
    private int numCorrect;
    private String mediaUrl;

    public void shuffleChoices() {
        Collections.shuffle(choices);
    }
}

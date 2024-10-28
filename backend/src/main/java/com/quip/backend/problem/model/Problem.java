package com.quip.backend.problem.model;

import com.quip.backend.problem.dto.ProblemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Problem extends ProblemDTO {
    private int correctAnswerIndex;
    private String createdAt;

    public Problem(long problemId, String question, List<String> choices, int numAsked, int numCorrect,
                   int correctAnswerIndex, String createdAt, String imgUrl) {
        super(problemId, question, choices, numAsked, numCorrect, imgUrl);
        this.correctAnswerIndex = correctAnswerIndex;
        this.createdAt = createdAt;
    }
}

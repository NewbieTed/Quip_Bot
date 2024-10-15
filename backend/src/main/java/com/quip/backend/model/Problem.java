package com.quip.backend.model;

import java.util.List;

public class Problem extends ProblemDTO {
    private int correctAnswerIndex;
    private String createdAt;

    public Problem(long id, String question, List<String> choices, int numAsked, int numCorrect,
                   int correctAnswerIndex, String createdAt, String imgUrl) {
        super(id, question, choices, numAsked, numCorrect, imgUrl);
        this.correctAnswerIndex = correctAnswerIndex;
        this.createdAt = createdAt;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public void setCorrectAnswerIndex(int correctAnswerIndex) {
        this.correctAnswerIndex = correctAnswerIndex;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

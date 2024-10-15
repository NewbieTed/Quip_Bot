package com.quip.backend.model;

import java.util.List;

public class ProblemDTO {
    private long id;
    private String question;
    private List<String> choices;
    private int numAsked;
    private int numCorrect;
    private String imgUrl;

    public ProblemDTO(long id, String question, List<String> choices, int numAsked, int numCorrect, String imgUrl) {
        this.id = id;
        this.question = question;
        this.choices = choices;
        this.numAsked = numAsked;
        this.numCorrect = numCorrect;
        this.imgUrl = imgUrl;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String username) {
        this.question = question;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }


    public int getNumAsked() {
        return numAsked;
    }

    public void setNumAsked(int numAsked) {
        this.numAsked = numAsked;
    }

    public void incrementNumAsked() {
        numAsked += 1;
    }


    public int getNumCorrect() {
        return numCorrect;
    }

    public void setNumCorrect(int numCorrect) {
        this.numCorrect = numCorrect;
    }

    public void incrementNumCorrect() {
        numCorrect += 1;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }
}

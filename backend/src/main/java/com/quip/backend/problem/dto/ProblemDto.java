package com.quip.backend.problem.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDto {
    private String question;
    private List<ProblemChoiceDto> choices;
    private String mediaUrl;
}
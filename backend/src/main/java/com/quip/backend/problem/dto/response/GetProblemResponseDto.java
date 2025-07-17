package com.quip.backend.problem.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetProblemResponseDto {
    private String question;
    private List<GetProblemChoiceResponseDto> choices;
    private String mediaUrl;
}
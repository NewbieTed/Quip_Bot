package com.quip.backend.problem.dto.request;


import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProblemRequestDto {
    @NotBlank(message = "Question cannot be blank")
    private String question;

    @Valid
    @NotEmpty(message = "Choices cannot be empty")
    private List<@NotNull(message = "Each choice cannot be null") CreateProblemChoiceRequestDto> choices;

    private Long mediaFileId;

    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    @NotNull(message = "Problem category ID cannot be null")
    @PositiveOrZero(message = "Problem category ID must be a non negative number")
    private Long problemCategoryId;

    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;
}
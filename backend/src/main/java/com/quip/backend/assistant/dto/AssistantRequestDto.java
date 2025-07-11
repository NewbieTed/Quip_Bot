package com.quip.backend.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssistantRequestDto {
    @NotBlank
    private String message;

    @PositiveOrZero
    private Long memberId;
}

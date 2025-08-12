package com.quip.backend.assistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;
}

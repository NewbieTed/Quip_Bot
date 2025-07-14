package com.quip.backend.problem.dto.request;

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
public class CreateProblemCategoryRequestDto {
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    @NotNull(message = "Problem Category Name cannot be null")
    @NotBlank(message = "Problem Category Name not be an empty string")
    private String problemCategoryName;

    @NotNull(message = "Problem Category Description cannot be null")
    @NotBlank(message = "Problem Category Description not be an empty string")
    private String problemCategoryDescription;
}

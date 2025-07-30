package com.quip.backend.tool.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateToolWhitelistRequestDto {
    /**
     * ID of the member making this request.
     * Used for authorization checks as well as whitelist update.
     */
    @NotNull(message = "Member ID cannot be null")
    @PositiveOrZero(message = "Member ID must be a non negative number")
    private Long memberId;

    /**
     * ID of the channel where this request is made.
     * Used for authorization checks as well as whitelist update.
     */
    @NotNull(message = "Channel ID cannot be null")
    @PositiveOrZero(message = "Channel ID must be a non negative number")
    private Long channelId;

    @Valid
    List<AddToolWhitelistRequestDto> addRequests;

    @Valid
    List<RemoveToolWhitelistRequestDto> removeRequests;
}

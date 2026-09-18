package com.emmagax.eden.dto;

import jakarta.validation.constraints.NotNull;

public record CreateConnectionRequestRequest(
    @NotNull(message = "From profile ID is required")
    Long fromProfileId,

    @NotNull(message = "To profile ID is required")
    Long toProfileId
) {
}

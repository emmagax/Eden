package com.emmagax.eden.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProfileRequest(
    @NotBlank(message = "Artist name is required") @Size(max = 20, message = "Artist name must be 20 characters or fewer") String artistName,

    @Size(max = 10, message = "Pronouns must be 10 characters or fewer") String pronouns,

    @Size(max = 20, message = "Zone must be 20 characters or fewer") String zone,

    @Size(max = 100, message = "Bio must be 100 characters or fewer") String bio) {
}

package com.emmagax.eden.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank(message = "Artist name is required") @Size(max = 20, message = "Artist name must be 20 characters or fewer") String artistName,

    @NotBlank(message = "Handle is required") @Size(min = 3, max = 30, message = "Handle must be between 3 and 30 characters") @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Handle can only contain letters, numbers, underscores, and hyphens") String handle,

    @Size(max = 10, message = "Pronouns must be 10 characters or fewer") String pronouns,

    @Size(max = 20, message = "Zone must be 20 characters or fewer") String zone,

    @Size(max = 100, message = "Bio must be 100 characters or fewer") String bio,

    @Size(max = 255, message = "Roles must be 255 characters or fewer") String roles,

    @Size(max = 255, message = "Genres must be 255 characters or fewer") String genres,

    @Size(max = 60, message = "Scene must be 60 characters or fewer") String scene,

    @Size(max = 500, message = "Avatar URL must be 500 characters or fewer") String avatarUrl,

    boolean onboardingComplete) {
}

package com.emmagax.eden.dto;

public record ProfileResponse(
    Long id,
    String artistName,
    String handle,
    String pronouns,
    String zone,
    String bio,
    String roles,
    String genres,
    String scene,
    String avatarUrl,
    boolean onboardingComplete,
    PublicUserResponse user) {
}

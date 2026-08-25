package com.emmagax.eden.dto;

public record ProfileResponse(Long id, String artistName, String pronouns, String zone, String bio, PublicUserResponse user) {
}

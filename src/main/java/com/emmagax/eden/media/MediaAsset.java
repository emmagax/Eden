package com.emmagax.eden.media;

import java.util.UUID;

public record MediaAsset(UUID id, long ownerId, String uploadKey, String contentType,
    long sizeBytes, String sha256, Integer previewSeconds, String state, int attempts,
    UUID leaseToken, String streamKey, String previewKey, Double durationSeconds, String failureCode) {}

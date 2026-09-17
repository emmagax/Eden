package com.emmagax.eden.dto;

import com.emmagax.eden.model.RequestStatus;

public record ConnectionRequestResponse(
    Long id,
    ProfileResponse fromProfile,
    ProfileResponse toProfile,
    RequestStatus status
) {
}

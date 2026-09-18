package com.emmagax.eden.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank(message = "Identifier is required") String identifier) {

}

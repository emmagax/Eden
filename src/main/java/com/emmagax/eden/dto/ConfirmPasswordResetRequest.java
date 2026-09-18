package com.emmagax.eden.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordResetRequest(@NotBlank(message = "Token is required") String token,
    @NotBlank(message = "Password is required") @Size(min = 8, max = 20, message = "Password must be netween 8 and 20 characters") String newPassword) {

}

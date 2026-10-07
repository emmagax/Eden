package com.emmagax.eden.exception;

import com.emmagax.eden.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(org.springframework.security.core.AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiErrorResponse("INVALID_CREDENTIALS", null, "Invalid credentials"));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException exception) {
        var error = exception.getBindingResult().getFieldErrors().stream().findFirst();
        return ResponseEntity.badRequest().body(new ApiErrorResponse("VALIDATION_FAILED",
                error.map(org.springframework.validation.FieldError::getField).orElse(null),
                error.map(org.springframework.validation.FieldError::getDefaultMessage).orElse("Invalid request")));
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleStatus(org.springframework.web.server.ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ApiErrorResponse("REQUEST_REJECTED", null,
                exception.getReason() == null ? "Request rejected" : exception.getReason()));
    }
    @ExceptionHandler(DuplicateAccountFieldException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateAccountField(DuplicateAccountFieldException exception) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(exception.getCode(), exception.getField(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

}

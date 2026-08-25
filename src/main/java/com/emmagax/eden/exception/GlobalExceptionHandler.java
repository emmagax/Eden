package com.emmagax.eden.exception;

import com.emmagax.eden.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DuplicateAccountFieldException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateAccountField(DuplicateAccountFieldException exception) {
        ApiErrorResponse errorResponse = new ApiErrorResponse(exception.getCode(), exception.getField(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

}

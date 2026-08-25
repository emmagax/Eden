package com.emmagax.eden.exception;

public class DuplicateAccountFieldException extends RuntimeException {

    private final String code;
    private final String field;

    public DuplicateAccountFieldException(String code, String field, String message) {
        super(message);
        this.code = code;
        this.field = field;
    }

    public String getCode() {
        return code;
    }

    public String getField() {
        return field;
    }
}

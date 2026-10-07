package com.emmagax.eden.dto;

public class LoginRequest {

    @jakarta.validation.constraints.NotBlank
    private String identifier;
    @jakarta.validation.constraints.NotBlank
    private String password;

    public LoginRequest() {}

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}

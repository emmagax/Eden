package com.emmagax.eden.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(mappedBy = "user")
  private Profile profile;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String username;

  private String password;

  @Column(name = "google_id", unique = true)
  private String googleId;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name = "email_verification_token_hash")
  private String emailVerificationTokenHash;

  @Column(name = "email_verification_token_expires_at")
  private LocalDateTime emailVerificationTokenExpiresAt;

  @Column(name = "password_reset_token_hash")
  private String passwordResetTokenHash;

  @Column(name = "password_reset_token_expires_at")
  private LocalDateTime passwordResetTokenExpiresAt;

  public User() {

  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getGoogleId() {
    return googleId;
  }

  public void setGoogleId(String googleId) {
    this.googleId = googleId;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }

  public void setEmailVerified(boolean emailVerified) {
    this.emailVerified = emailVerified;
  }

  public String getEmailVerificationTokenHash() {
    return emailVerificationTokenHash;
  }

  public void setEmailVerificationTokenHash(String emailVerificationTokenHash) {
    this.emailVerificationTokenHash = emailVerificationTokenHash;
  }

  public LocalDateTime getEmailVerificationTokenExpiresAt() {
    return emailVerificationTokenExpiresAt;
  }

  public void setEmailVerificationTokenExpiresAt(LocalDateTime emailVerificationTokenExpiresAt) {
    this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt;
  }

  public String getPasswordResetTokenHash() {
    return passwordResetTokenHash;
  }

  public void setPasswordResetTokenHash(String passwordResetTokenHash) {
    this.passwordResetTokenHash = passwordResetTokenHash;
  }

  public LocalDateTime getPasswordResetTokenExpiresAt() {
    return passwordResetTokenExpiresAt;
  }

  public void setPasswordResetTokenExpiresAt(LocalDateTime passwordResetTokenExpiresAt) {
    this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
  }
}

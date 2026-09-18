ALTER TABLE users
  ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN email_verification_token_hash VARCHAR(255),
  ADD COLUMN email_verification_token_expires_at TIMESTAMP,
  ADD COLUMN password_reset_token_hash VARCHAR(255),
  ADD COLUMN password_reset_token_expires_at TIMESTAMP;

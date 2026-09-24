ALTER TABLE profiles
  ADD COLUMN handle VARCHAR(30),
  ADD COLUMN roles VARCHAR(255),
  ADD COLUMN genres VARCHAR(255),
  ADD COLUMN scene VARCHAR(60),
  ADD COLUMN avatar_url VARCHAR(500),
  ADD COLUMN onboarding_complete BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE profiles
  ADD CONSTRAINT profiles_handle_unique UNIQUE (handle);

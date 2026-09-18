package com.emmagax.eden.repository;

import com.emmagax.eden.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByUsername(String username);

  Optional<User> findByEmailVerificationTokenHash(String emailVerificationTokenHash);

  Optional<User> findByPasswordResetTokenHash(String passwordResetTokenHash);

  boolean existsByEmail(String email);

  boolean existsByUsername(String username);
}

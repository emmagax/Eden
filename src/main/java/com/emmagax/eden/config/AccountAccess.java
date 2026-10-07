package com.emmagax.eden.config;

import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountAccess {
  private final UserRepository users;
  public AccountAccess(UserRepository users) { this.users = users; }

  public User current(Authentication authentication) {
    if (authentication == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    return users.findByUsername(authentication.getName())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  }

  public void requireOwner(Authentication authentication, User owner) {
    if (!current(authentication).getId().equals(owner.getId()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not own this resource");
  }
}

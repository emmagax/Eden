package com.emmagax.eden.config;

import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EdenUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public EdenUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;

  }

  @Override
  public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(identifier).or(() -> userRepository.findByUsername(identifier))
        .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
        .password(user.getPassword()).roles("USER").build();
  }
}

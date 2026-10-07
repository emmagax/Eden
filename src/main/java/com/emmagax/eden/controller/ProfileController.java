package com.emmagax.eden.controller;

import com.emmagax.eden.dto.ProfileResponse;
import com.emmagax.eden.dto.PublicUserResponse;
import com.emmagax.eden.model.Profile;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.ProfileRepository;
import org.springframework.web.bind.annotation.*;
import com.emmagax.eden.dto.CreateProfileRequest;
import com.emmagax.eden.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.util.List;
import com.emmagax.eden.config.AccountAccess;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

  private final ProfileRepository profileRepository;
  private final AccountAccess access;

  public ProfileController(
      ProfileRepository profileRepository, AccountAccess access) {
    this.profileRepository = profileRepository;
    this.access = access;
  }

  @GetMapping
  public List<ProfileResponse> getAll() {
    return profileRepository.findAll().stream().map(this::toProfileResponse).toList();
  }

  @PostMapping("/users/{userId}/profile")
  public ProfileResponse create(
      @PathVariable Long userId,
      @Valid @RequestBody CreateProfileRequest request, Authentication authentication) {
    User user = access.current(authentication);
    if (!user.getId().equals(userId))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);

    Profile profile = new Profile();
    profile.setUser(user);
    profile.setArtistName(request.artistName());
    profile.setHandle(request.handle());
    profile.setPronouns(request.pronouns());
    profile.setZone(request.zone());
    profile.setBio(request.bio());
    profile.setRoles(request.roles());
    profile.setGenres(request.genres());
    profile.setScene(request.scene());
    profile.setAvatarUrl(request.avatarUrl());

    Profile savedProfile = profileRepository.save(profile);
    return toProfileResponse(savedProfile);
  }

  @PutMapping("/{profileId}")
  public ProfileResponse update(
      @PathVariable Long profileId,
      @Valid @RequestBody UpdateProfileRequest request, Authentication authentication) {
    Profile profile = profileRepository.findById(profileId)
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    access.requireOwner(authentication, profile.getUser());

    profile.setArtistName(request.artistName());
    profile.setHandle(request.handle());
    profile.setPronouns(request.pronouns());
    profile.setZone(request.zone());
    profile.setBio(request.bio());
    profile.setRoles(request.roles());
    profile.setGenres(request.genres());
    profile.setScene(request.scene());
    profile.setAvatarUrl(request.avatarUrl());
    profile.setOnboardingComplete(request.onboardingComplete());
    Profile savedProfile = profileRepository.save(profile);
    return toProfileResponse(savedProfile);
  }

  private ProfileResponse toProfileResponse(Profile profile) {
    User user = profile.getUser();

    PublicUserResponse publicUser = new PublicUserResponse(
        user.getId(),
        user.getUsername());
    return new ProfileResponse(
        profile.getId(),
        profile.getArtistName(),
        profile.getHandle(),
        profile.getPronouns(),
        profile.getZone(),
        profile.getBio(),
        profile.getRoles(),
        profile.getGenres(),
        profile.getScene(),
        profile.getAvatarUrl(),
        profile.isOnboardingComplete(),
        publicUser);
  }

}

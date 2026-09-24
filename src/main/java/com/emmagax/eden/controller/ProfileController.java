package com.emmagax.eden.controller;

import com.emmagax.eden.dto.ProfileResponse;
import com.emmagax.eden.dto.PublicUserResponse;
import com.emmagax.eden.model.Profile;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.ProfileRepository;
import com.emmagax.eden.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import com.emmagax.eden.dto.CreateProfileRequest;
import com.emmagax.eden.dto.UpdateProfileRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

  private final ProfileRepository profileRepository;
  private final UserRepository userRepository;

  public ProfileController(
      ProfileRepository profileRepository,
      UserRepository userRepository) {
    this.profileRepository = profileRepository;
    this.userRepository = userRepository;
  }

  @GetMapping
  public List<ProfileResponse> getAll() {
    return profileRepository.findAll().stream().map(this::toProfileResponse).toList();
  }

  @PostMapping("/users/{userId}/profile")
  public ProfileResponse create(
      @PathVariable Long userId,
      @Valid @RequestBody CreateProfileRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

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
      @Valid @RequestBody UpdateProfileRequest request) {
    Profile profile = profileRepository.findById(profileId)
        .orElseThrow(() -> new RuntimeException("Profile not found"));

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

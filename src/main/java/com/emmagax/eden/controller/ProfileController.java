package com.emmagax.eden.controller;


import com.emmagax.eden.dto.ProfileResponse;
import com.emmagax.eden.dto.PublicUserResponse;
import com.emmagax.eden.model.Profile;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.ProfileRepository;
import com.emmagax.eden.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileController(
            ProfileRepository profileRepository,
            UserRepository userRepository
    ) {
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
            @RequestBody Profile profile
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        profile.setUser(user);
        Profile savedProfile = profileRepository.save(profile);
        return toProfileResponse(savedProfile);
    }

    @PutMapping("/{profileId}")
    public ProfileResponse update(
            @PathVariable Long profileId,
            @RequestBody Profile updates
    ) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setArtistName(updates.getArtistName());
        profile.setPronouns(updates.getPronouns());
        profile.setZone(updates.getZone());
        profile.setBio(updates.getBio());
        Profile savedProfile = profileRepository.save(profile);
        return toProfileResponse(savedProfile);
    }

    private ProfileResponse toProfileResponse(Profile profile) {
        User user = profile.getUser();

        PublicUserResponse publicUser = new PublicUserResponse(
                user.getId(),
                user.getUsername()
        );
        return new ProfileResponse(profile.getId(), profile.getArtistName(), profile.getPronouns(), profile.getZone(), profile.getBio(), publicUser);
    }

}

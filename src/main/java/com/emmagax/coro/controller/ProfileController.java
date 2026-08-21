package com.emmagax.coro.controller;


import com.emmagax.coro.model.Profile;
import com.emmagax.coro.model.User;
import com.emmagax.coro.repository.ProfileRepository;
import com.emmagax.coro.repository.UserRepository;
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
    public List<Profile> getAll() {
        return profileRepository.findAll();
    }

    @PostMapping("/users/{userId}/profile")
    public Profile create(
            @PathVariable Long userId,
            @RequestBody Profile profile
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        profile.setUser(user);
        return profileRepository.save(profile);
    }

    @PutMapping("/{profileId}")
    public Profile update(
            @PathVariable Long profileId,
            @RequestBody Profile updates
    ) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setArtistName(updates.getArtistName());
        profile.setPronouns(updates.getPronouns());
        profile.setZone(updates.getZone());
        profile.setBio(updates.getBio());

        return profileRepository.save(profile);
    }

}

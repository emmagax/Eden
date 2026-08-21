package com.emmagax.coro.controller;


import com.emmagax.coro.model.Profile;
import com.emmagax.coro.repository.ProfileRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileRepository profileRepository;

    public ProfileController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public List<Profile> getAll() {
        return profileRepository.findAll();
    }

    @PostMapping
    public Profile create(@RequestBody Profile profile) {
        return profileRepository.save(profile);
    }

}

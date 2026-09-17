package com.emmagax.eden.controller;

import com.emmagax.eden.dto.ConnectionRequestResponse;
import com.emmagax.eden.dto.CreateConnectionRequestRequest;
import com.emmagax.eden.dto.ProfileResponse;
import com.emmagax.eden.dto.PublicUserResponse;
import com.emmagax.eden.model.ConnectionRequest;
import com.emmagax.eden.model.Profile;
import com.emmagax.eden.model.User;
import com.emmagax.eden.repository.ConnectionRequestRepository;
import com.emmagax.eden.repository.ProfileRepository;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/connection-requests")
public class ConnectionRequestController {

    private final ConnectionRequestRepository connectionRequestRepository;
    private final ProfileRepository profileRepository;

    public ConnectionRequestController(
            ConnectionRequestRepository connectionRequestRepository,
            ProfileRepository profileRepository
    ) {
        this.connectionRequestRepository = connectionRequestRepository;
        this.profileRepository = profileRepository;
    }

    @GetMapping
    public List<ConnectionRequestResponse> getAll() {
        return connectionRequestRepository.findAll()
                .stream()
                .map(this::toConnectionRequestResponse)
                .toList();
    }

    @PostMapping
    public ConnectionRequestResponse create(
            @Valid @RequestBody CreateConnectionRequestRequest request
    ) {
        Profile fromProfile = profileRepository.findById(request.fromProfileId())
                .orElseThrow(() -> new RuntimeException("From profile not found"));
        Profile toProfile = profileRepository.findById(request.toProfileId())
                .orElseThrow(() -> new RuntimeException("To profile not found"));

        ConnectionRequest connectionRequest = new ConnectionRequest();
        connectionRequest.setFromProfile(fromProfile);
        connectionRequest.setToProfile(toProfile);

        ConnectionRequest savedConnectionRequest = connectionRequestRepository.save(connectionRequest);
        return toConnectionRequestResponse(savedConnectionRequest);
    }

    private ConnectionRequestResponse toConnectionRequestResponse(ConnectionRequest connectionRequest) {
        return new ConnectionRequestResponse(
                connectionRequest.getId(),
                toProfileResponse(connectionRequest.getFromProfile()),
                toProfileResponse(connectionRequest.getToProfile()),
                connectionRequest.getStatus()
        );
    }

    private ProfileResponse toProfileResponse(Profile profile) {
        User user = profile.getUser();
        PublicUserResponse publicUser = new PublicUserResponse(
                user.getId(),
                user.getUsername()
        );

        return new ProfileResponse(
                profile.getId(),
                profile.getArtistName(),
                profile.getPronouns(),
                profile.getZone(),
                profile.getBio(),
                publicUser
        );
    }
}

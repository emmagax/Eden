package com.emmagax.eden.controller;

import com.emmagax.eden.model.ConnectionRequest;
import com.emmagax.eden.repository.ConnectionRequestRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/connection-requests")
public class ConnectionRequestController {

    private final ConnectionRequestRepository connectionRequestRepository;

    public ConnectionRequestController(ConnectionRequestRepository connectionRequestRepository) {
        this.connectionRequestRepository = connectionRequestRepository;
    }

    @GetMapping
    public List<ConnectionRequest> getAll() {
        return connectionRequestRepository.findAll();
    }

    @PostMapping
    public ConnectionRequest create(@RequestBody ConnectionRequest connectionRequest) {
        return connectionRequestRepository.save(connectionRequest);
    }

}

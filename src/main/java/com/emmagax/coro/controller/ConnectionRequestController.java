package com.emmagax.coro.controller;

import com.emmagax.coro.model.ConnectionRequest;
import com.emmagax.coro.repository.ConnectionRequestRepository;
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

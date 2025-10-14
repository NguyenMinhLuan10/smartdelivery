package com.smartdelivery.driver.controller;

import com.smartdelivery.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/internal/drivers")
@RequiredArgsConstructor
public class DriverInternalController {
    private final DriverRepository drivers;

    @GetMapping("/by-user/{userId}")
    public DriverLookupResponse byUser(@PathVariable("userId") UUID userId) { // 👈 thêm ("userId")
        var d = drivers.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found for userId"));
        return new DriverLookupResponse(d.getId(), d.getUserId());
    }

    public record DriverLookupResponse(UUID id, UUID userId) {}
}


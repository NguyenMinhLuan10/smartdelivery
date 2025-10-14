package com.smartdelivery.shipment.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DriverClient {

    private final RestClient driverRestClient;

    /** bearer: "Bearer xxx" (có thể null) */
    public UUID resolveDriverId(UUID userId, String bearer) {
        try {
            var req = driverRestClient.get()
                    // Gọi qua gateway: /internal/drivers/by-user/{userId}
                    .uri("/internal/drivers/by-user/{userId}", userId);

            if (bearer != null && !bearer.isBlank()) {
                req = req.header(HttpHeaders.AUTHORIZATION, bearer);
            }

            var res = req.retrieve().body(DriverLookupResponse.class);
            if (res == null || res.id == null) {
                throw new IllegalStateException("Driver not found for userId=" + userId);
            }
            return res.id;
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new RuntimeException("Unauthorized calling driver-service (missing/invalid token).", e);
        }
    }

    public record DriverLookupResponse(UUID id, UUID userId) {}
}

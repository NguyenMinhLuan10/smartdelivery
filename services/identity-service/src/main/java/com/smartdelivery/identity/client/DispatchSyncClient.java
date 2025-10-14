// identity-service: client/DispatchSyncClient.java
package com.smartdelivery.identity.client;

import com.smartdelivery.identity.dto.DriverSyncResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DispatchSyncClient {
    private final RestTemplate rest;

    @Value("${app.gateway.base-url:http://localhost:8085}")
    private String gw;

    public UUID syncDriver(UUID userId, String name, String phone, String email, String bearerToken) {
        var url = gw + "/internal/drivers/sync";

        HttpHeaders h = new HttpHeaders();
        if (bearerToken != null && !bearerToken.isBlank()) {
            h.setBearerAuth(bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken);
        }
        h.setContentType(MediaType.APPLICATION_JSON);

        var body = Map.of(
                "userId", userId,
                "fullName", name,
                "phone", phone,
                "email", email
        );

        ResponseEntity<DriverSyncResponse> resp = rest.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, h), DriverSyncResponse.class);

        var dto = resp.getBody();
        if (dto == null || !dto.isSynced() || dto.getDriverId() == null) {
            throw new IllegalStateException("Sync driver failed or missing driverId");
        }
        return dto.getDriverId();
    }
}

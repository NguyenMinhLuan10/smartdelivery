package com.smartdelivery.shipment.client;

import com.smartdelivery.shipment.dto.OrderStatusChangeRequest;
import com.smartdelivery.shipment.dto.OrderTrackResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;            // 👈 THÊM
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderClient {
    private final RestTemplate rest;
    @Value("${app.gateway.base-url:http://localhost:8085}") private String gw;

    public OrderTrackResponse getByTracking(String code, String auth){
        HttpHeaders h = new HttpHeaders(); h.setBearerAuth(extractToken(auth));
        var req = new HttpEntity<>(h);
        var url = gw + "/orders/track?code={code}";
        return rest.exchange(url, HttpMethod.GET, req, OrderTrackResponse.class, code).getBody();
    }

    public void transition(UUID orderId, OrderStatusChangeRequest body, String auth){
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(extractToken(auth));
        var req = new HttpEntity<>(body, h);
        var url = gw + "/internal/orders/{id}/status";
        rest.exchange(url, HttpMethod.POST, req, Void.class, orderId);
    }

    // 👇 THÊM: gọi order-service để lưu driver đã assign (đồng bộ DB)
    public void setAssignedDriver(UUID orderId, UUID driverId, String auth){
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(extractToken(auth));
        var req = new HttpEntity<>(Map.of("driverId", driverId), h);
        var url = gw + "/internal/orders/{id}/assign-driver";
        rest.exchange(url, HttpMethod.POST, req, Void.class, orderId);
    }

    private String extractToken(String authHeader){
        if (authHeader == null) throw new IllegalStateException("Missing Authorization");
        return authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
    }
}

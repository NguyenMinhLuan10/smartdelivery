package com.smartdelivery.order.controller;

import com.smartdelivery.order.dto.StatusChangeRequest;
import com.smartdelivery.order.firebase.FirebaseGateway;
import com.smartdelivery.order.model.Order;
import com.smartdelivery.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {
    private final OrderService svc;
    private final FirebaseGateway fb;

    @PostMapping("/{id}/status")
    public Map<String,Object> transition(@PathVariable("id") UUID id,
                                         @Valid @RequestBody StatusChangeRequest req) {
        Order ord = svc.transition(id, req);
        fb.upsertOrder(
                ord.getId(),
                ord.getTrackingCode(),
                ord.getStatus(),
                ord.toGeoMap(),
                ord.getAssignedDriverId()
        );
        return Map.of("updated", true);
    }

    @PostMapping("/{id}/assign-driver")
    public Map<String,Object> assignDriver(@PathVariable("id") UUID id,
                                           @RequestBody Map<String, UUID> body) {
        UUID driverId = body.get("driverId");
        Order ord = svc.assignDriver(id, driverId);
        fb.upsertOrder(
                ord.getId(),
                ord.getTrackingCode(),
                ord.getStatus(),
                ord.toGeoMap(),
                ord.getAssignedDriverId()
        );
        return Map.of("assigned", true);
    }
}

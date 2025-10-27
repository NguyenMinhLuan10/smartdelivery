package com.smartdelivery.order.controller;

import com.smartdelivery.order.dto.StatusChangeRequest;
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

    // dùng cho tác vụ nội bộ (dispatcher/driver app) khi quét QR v.v.
    @PostMapping("/{id}/status")
    public Map<String,Object> transition(
            @PathVariable("id") UUID id,
            @Valid @RequestBody StatusChangeRequest req) {
        svc.transition(id, req);
        return Map.of("updated", true);
    }
}

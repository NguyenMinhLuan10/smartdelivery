package com.smartdelivery.shipment.controller;

import com.smartdelivery.shipment.dto.*;
import com.smartdelivery.shipment.service.DriverFlowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/driver")
@RequiredArgsConstructor
public class DriverOpsController {

    private final DriverFlowService flow;

    @GetMapping("/tasks")
    @PreAuthorize("hasRole('DRIVER')")
    public List<TaskSummaryResponse> myTasks(Authentication auth, HttpServletRequest req) {
        return flow.myTasks(auth, req);
    }

    @PostMapping("/tasks/{taskId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Object> accept(@PathVariable UUID taskId, Authentication auth, HttpServletRequest req) {
        flow.accept(taskId, auth, req);
        return Map.of("accepted", true);
    }

    @PostMapping("/tasks/{taskId}/reject")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Object> reject(@PathVariable UUID taskId, Authentication auth, HttpServletRequest req) {
        flow.reject(taskId, auth, req);
        return Map.of("rejected", true);
    }

    @PostMapping("/scan/pickup")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Object> pickup(Authentication auth, HttpServletRequest req, @Valid @RequestBody PickupScanRequest body) {
        flow.scanPickup(body, auth, req);
        return Map.of("pickedUp", true);
    }

    @PostMapping("/scan/out-for-delivery")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Object> ofd(Authentication auth, HttpServletRequest req, @Valid @RequestBody OutForDeliveryRequest body) {
        flow.outForDelivery(body, auth, req);
        return Map.of("outForDelivery", true);
    }

    @PostMapping("/scan/delivered")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Object> delivered(Authentication auth, HttpServletRequest req, @Valid @RequestBody DeliveredRequest body) {
        flow.delivered(body, auth, req);
        return Map.of("delivered", true);
    }

    @PostMapping("/scan/failed")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Object> failed(Authentication auth, HttpServletRequest req, @Valid @RequestBody FailedRequest body) {
        flow.failed(body, auth, req);
        return Map.of("failed", true);
    }

    @PostMapping("/scan/inbound-hub")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String, Object> inboundHub(Authentication auth, HttpServletRequest req, @Valid @RequestBody HubScanRequest body) {
        flow.scanInboundHub(body, auth, req);
        return Map.of("inbound", true);
    }
}

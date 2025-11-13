package com.smartdelivery.shipment.service;

import com.smartdelivery.shipment.client.DriverClient;
import com.smartdelivery.shipment.client.OrderClient;
import com.smartdelivery.shipment.dto.*;
import com.smartdelivery.shipment.firebase.FirebaseGateway;
import com.smartdelivery.shipment.model.Pod;
import com.smartdelivery.shipment.model.DriverTask;
import com.smartdelivery.shipment.repository.DriverTaskRepository;
import com.smartdelivery.shipment.repository.HubRepository;
import com.smartdelivery.shipment.repository.LegRepository;
import com.smartdelivery.shipment.repository.PodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverFlowService {

    private final DriverTaskRepository tasks;
    private final PodRepository pods;
    private final OrderClient orderClient;
    private final LegRepository legs;
    private final HubRepository hubs;
    private final DriverClient driverClient;
    private final FirebaseGateway fb;

    // ===== helpers =====
    private static String bearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return (h != null && !h.isBlank()) ? h : null;
    }

    private UUID requireDriverId(Authentication auth, String bearer) {
        final UUID userId;
        try {
            userId = UUID.fromString(auth.getName());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid subject");
        }
        try {
            UUID driverId = driverClient.resolveDriverId(userId, bearer);
            log.info("Resolved driverId={} from userId={}", driverId, userId);
            return driverId;
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ex.getMessage());
        }
    }

    private void requireLegType(UUID legId, String type) {
        var leg = legs.findById(legId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leg not found: " + legId));
        if (!type.equalsIgnoreCase(leg.getLegType())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Require legType=" + type + " but was " + leg.getLegType()
            );
        }
    }

    private void transitionLeg(UUID legId, String to, boolean setStart, boolean setEnd) {
        var leg = legs.findById(legId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leg not found: " + legId));
        String from = leg.getStatus();
        leg.setStatus(to);
        if (setStart && leg.getActualStart() == null) {
            leg.setActualStart(OffsetDateTime.now());
        }
        if (setEnd) {
            leg.setActualEnd(OffsetDateTime.now());
        }
        legs.save(leg);
        log.info("transitionLeg(): {} -> {} (legId={})", from, to, legId);
    }

    private boolean matchesDriverOrUser(UUID taskDriverId, UUID driverId, UUID userId) {
        return taskDriverId.equals(driverId) || taskDriverId.equals(userId);
    }

    private String findLegType(UUID legId) {
        if (legId == null) return null;
        return legs.findById(legId).map(l -> l.getLegType()).orElse(null);
    }

    // ================== QUERIES ==================
    public List<TaskSummaryResponse> myTasks(Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());
        log.info("myTasks(): driverId={}, userId={}", driverId, userId);

        List<DriverTask> result = tasks.findByDriverIdOrderByAssignedAtDesc(driverId);
        if (result.isEmpty()) {
            result = tasks.findByDriverIdOrderByAssignedAtDesc(userId);
            if (!result.isEmpty()) {
                log.warn("myTasks(): fallback matched by userId (DB chưa migrate).");
            }
        }
        if (result.isEmpty()) {
            log.info("myTasks(): found 0 tasks for driverId={}", driverId);
            return Collections.emptyList();
        }

        final Set<UUID> legIds = result.stream()
                .map(DriverTask::getLegId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        final Map<UUID, String> legTypes;
        if (!legIds.isEmpty()) {
            var legEntities = legs.findAllById(legIds);
            legTypes = legEntities.stream()
                    .collect(Collectors.toMap(
                            l -> l.getId(),
                            l -> l.getLegType()
                    ));
        } else {
            legTypes = Collections.emptyMap();
        }

        List<TaskSummaryResponse> responses = result.stream()
                .map(t -> TaskSummaryResponse.builder()
                        .taskId(t.getId())
                        .legId(t.getLegId())
                        .legType(t.getLegId() != null ? legTypes.get(t.getLegId()) : null)
                        .trackingCode(t.getTrackingCode())
                        .status(t.getStatus())
                        .assignedAt(t.getAssignedAt())
                        .build())
                .toList();

        log.info("myTasks(): found {} tasks for driverId={}", responses.size(), driverId);
        return responses;
    }

    // ================== ACTIONS ==================

    @Transactional
    public void accept(UUID taskId, Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());

        var t = tasks.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        if (!matchesDriverOrUser(t.getDriverId(), driverId, userId)) {
            log.error("accept(): Forbidden - driverId={} userId={} task.driverId={}",
                    driverId, userId, t.getDriverId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to current driver");
        }
        if (!"ASSIGNED".equalsIgnoreCase(t.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid state: " + t.getStatus());
        }

        t.setStatus("ACCEPTED");
        t.setStartTime(OffsetDateTime.now());
        tasks.save(t);

        fb.updateDriverTaskStatus(t.getDriverId(), t.getId(), t.getStatus());
    }

    @Transactional
    public void reject(UUID taskId, Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());

        var t = tasks.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found: " + taskId));

        if (!matchesDriverOrUser(t.getDriverId(), driverId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to current driver");
        }

        t.setStatus("CANCELLED");
        tasks.save(t);

        fb.deleteDriverTask(t.getDriverId(), t.getId());
    }

    @Transactional
    public void scanPickup(PickupScanRequest body, Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());

        var t = tasks.findById(body.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + body.getTaskId()));

        if (!matchesDriverOrUser(t.getDriverId(), driverId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to current driver");
        }
        requireLegType(t.getLegId(), "PICKUP");

        if (!t.getTrackingCode().equals(body.getTrackingCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking mismatch");
        }

        try {
            orderClient.transition(
                    t.getOrderId(),
                    new OrderStatusChangeRequest("PICKED_UP", "driver_scan_pickup"),
                    bearer
            );
        } catch (Exception ignored) {}

        t.setStatus("STARTED");
        t.setStartTime(OffsetDateTime.now());
        tasks.save(t);
        transitionLeg(t.getLegId(), "IN_TRANSIT", true, false);

        fb.updateDriverTaskStatus(t.getDriverId(), t.getId(), t.getStatus());
    }

    @Transactional
    public void outForDelivery(OutForDeliveryRequest body, Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());

        var t = tasks.findById(body.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + body.getTaskId()));

        if (!matchesDriverOrUser(t.getDriverId(), driverId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to current driver");
        }
        requireLegType(t.getLegId(), "DELIVERY");

        try {
            orderClient.transition(
                    t.getOrderId(),
                    new OrderStatusChangeRequest("OUT_FOR_DELIVERY", "driver_start_ofd"),
                    bearer
            );
        } catch (Exception ignored) {}

        if (!"STARTED".equalsIgnoreCase(t.getStatus())) {
            t.setStatus("STARTED");
        }
        tasks.save(t);
        transitionLeg(t.getLegId(), "IN_TRANSIT", true, false);

        fb.updateDriverTaskStatus(t.getDriverId(), t.getId(), t.getStatus());
    }

    @Transactional
    public void delivered(DeliveredRequest body, Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());

        var t = tasks.findById(body.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + body.getTaskId()));

        if (!matchesDriverOrUser(t.getDriverId(), driverId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to current driver");
        }
        requireLegType(t.getLegId(), "DELIVERY");

        pods.save(Pod.builder()
                .taskId(t.getId())
                .method(body.getMethod())
                .photoUrl(body.getPhotoUrl())
                .signedName(body.getSignedName())
                .signedPhone(body.getSignedPhone())
                .collectedCodAmount(body.getCodAmount())
                .build());

        t.setStatus("DELIVERED");
        t.setDeliveredTime(OffsetDateTime.now());
        tasks.save(t);
        transitionLeg(t.getLegId(), "COMPLETED", false, true);

        try {
            orderClient.transition(
                    t.getOrderId(),
                    new OrderStatusChangeRequest("DELIVERED", "driver_pod_ok"),
                    bearer
            );
        } catch (Exception ignored) {}

        fb.updateDriverTaskStatus(t.getDriverId(), t.getId(), t.getStatus());
    }

    @Transactional
    public void failed(FailedRequest body, Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());

        var t = tasks.findById(body.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + body.getTaskId()));

        if (!matchesDriverOrUser(t.getDriverId(), driverId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to current driver");
        }

        t.setStatus("FAILED");
        t.setFailedTime(OffsetDateTime.now());
        t.setFailReason(body.getReason());
        tasks.save(t);

        try {
            orderClient.transition(
                    t.getOrderId(),
                    new OrderStatusChangeRequest("FAILED", body.getReason()),
                    bearer
            );
        } catch (Exception ignored) {}

        fb.updateDriverTaskStatus(t.getDriverId(), t.getId(), t.getStatus());
    }

    @Transactional
    public void scanInboundHub(HubScanRequest body, Authentication auth, HttpServletRequest req) {
        final String bearer = bearer(req);
        final UUID driverId = requireDriverId(auth, bearer);
        final UUID userId = UUID.fromString(auth.getName());

        var t = tasks.findById(body.getTaskId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Task not found: " + body.getTaskId()));

        if (!matchesDriverOrUser(t.getDriverId(), driverId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task does not belong to current driver");
        }

        if (body.getTrackingCode() != null
                && !body.getTrackingCode().isBlank()
                && !t.getTrackingCode().equals(body.getTrackingCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tracking mismatch");
        }

        hubs.findByCode(body.getHubCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown hubCode"));

        try {
            orderClient.transition(
                    t.getOrderId(),
                    new OrderStatusChangeRequest("AT_ORIGIN_HUB", "driver_scan_inbound_origin_hub"),
                    bearer
            );
        } catch (Exception ignored) {}

        var leg = legs.findById(t.getLegId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Leg not found: " + t.getLegId()));

        // ✅ PICKUP leg hoàn tất
        if ("PICKUP".equalsIgnoreCase(leg.getLegType())) {
            if (!"COMPLETED".equalsIgnoreCase(leg.getStatus())) {
                leg.setStatus("COMPLETED");
                leg.setActualEnd(OffsetDateTime.now());
                legs.save(leg);
            }
        }

        // ✅ Task PICKUP chỉ = COMPLETED (không phải DELIVERED)
        if (!"COMPLETED".equalsIgnoreCase(t.getStatus())) {
            t.setStatus("COMPLETED");
            // không set deliveredTime cho pickup
            tasks.save(t);
        } else {
            tasks.save(t);
        }

        fb.updateDriverTaskStatus(t.getDriverId(), t.getId(), t.getStatus());
    }
}

package com.smartdelivery.shipment.service;

import com.smartdelivery.shipment.client.OrderClient;
import com.smartdelivery.shipment.dto.AssignRequest;
import com.smartdelivery.shipment.dto.OrderStatusChangeRequest;
import com.smartdelivery.shipment.dto.OrderTrackResponse;
import com.smartdelivery.shipment.dto.TaskSummaryResponse;
import com.smartdelivery.shipment.firebase.FirebaseGateway;
import com.smartdelivery.shipment.model.DriverTask;
import com.smartdelivery.shipment.model.Hub;
import com.smartdelivery.shipment.model.Leg;
import com.smartdelivery.shipment.repository.DriverTaskRepository;
import com.smartdelivery.shipment.repository.HubRepository;
import com.smartdelivery.shipment.repository.LegRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssignService {

    private final LegRepository legs;
    private final DriverTaskRepository tasks;
    private final OrderClient orderClient;
    private final PlannerService planner;
    private final FirebaseGateway fb;
    private final HubRepository hubs;   // 👈 THÊM

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public TaskSummaryResponse assign(AssignRequest req, HttpServletRequest http) {
        final String auth = http.getHeader("Authorization");

        // 1. lấy đơn
        final OrderTrackResponse order = orderClient.getByTracking(req.getTrackingCode(), auth);
        if (order == null) {
            throw new IllegalStateException("Order not found by tracking: " + req.getTrackingCode());
        }

        // 2. xác định loại leg
        final String requestedType = req.getType().toUpperCase(Locale.ROOT);
        String actualType = requestedType;
        if ("AUTO".equals(requestedType)) {
            var plan = planner.classify(order);
            actualType = planner.suggestLegTypeForAssignment(plan);
            log.info("assign(AUTO): classified={} -> legType={}", plan.kind(), actualType);
        }
        final String legTypeForQuery = actualType;

        // 3. tìm/tạo leg
        final List<Leg> allLegs = legs.findByOrderIdOrderBySeqNoAsc(order.getId());
        Leg leg = allLegs.stream()
                .filter(l -> legTypeForQuery.equalsIgnoreCase(l.getLegType()))
                .sorted(Comparator
                        .comparing((Leg l) -> scoreStatusForReuse(l.getStatus()))
                        .thenComparing(Leg::getSeqNo, Comparator.nullsLast(Comparator.naturalOrder())))
                .findFirst()
                .orElseGet(() -> legs.save(Leg.builder()
                        .orderId(order.getId())
                        .seqNo(nextSeqNo(order.getId()))
                        .legType(legTypeForQuery)
                        .status("PLANNED")
                        .build()));

        if (isAssignable(leg.getStatus())) {
            if (!"ASSIGNED".equalsIgnoreCase(leg.getStatus())) {
                leg.setStatus("ASSIGNED");
                leg = legs.save(leg);
            }
        } else {
            log.warn("assign(): leg {} already {}", leg.getId(), leg.getStatus());
        }

        // 4. tạo driver_task
        DriverTask task = DriverTask.builder()
                .legId(leg.getId())
                .orderId(order.getId())
                .trackingCode(order.getTrackingCode())
                .driverId(req.getDriverId())
                .status("ASSIGNED")
                .assignedAt(OffsetDateTime.now())
                .build();
        tasks.saveAndFlush(task);

        // 4b. ghi task tổng
        fb.upsertTask(
                task.getId(),
                order.getId(),
                order.getTrackingCode(),
                "ASSIGNED",
                req.getDriverId()
        );

        // 5. chọn toạ độ hiển thị chính (tuỳ leg)
        final boolean isPickupLeg = "PICKUP".equalsIgnoreCase(legTypeForQuery);

        final String displayAddress = isPickupLeg
                ? order.getPickupAddress()
                : order.getDropoffAddress();

        final Double displayLat = isPickupLeg
                ? toDouble(order.getPickupLat())
                : toDouble(order.getDropoffLat());

        final Double displayLng = isPickupLeg
                ? toDouble(order.getPickupLng())
                : toDouble(order.getDropoffLng());

        // 5b. LẤY HUB (chỉ cho PICKUP)
        String hubAddress = null;
        Double hubLat = null;
        Double hubLng = null;

        if (isPickupLeg) {
            // ưu tiên: nếu leg đã có fromHubId -> lấy hub đó
            Hub hub = null;
            if (leg.getFromHubId() != null) {
                hub = hubs.findById(leg.getFromHubId()).orElse(null);
            }

            // nếu leg chưa có hub thì lấy hub đầu tiên (hub cố định)
            if (hub == null) {
                Optional<Hub> anyHub = hubs.findAll().stream().findFirst();
                if (anyHub.isPresent()) {
                    hub = anyHub.get();
                    // gán lại vào leg để lần sau khỏi đoán
                    leg.setFromHubId(hub.getId());
                    legs.save(leg);
                }
            }

            if (hub != null) {
                hubAddress = hub.getAddressText();
                hubLat = hub.getLat();
                hubLng = hub.getLng();
            }
        }

        // 6. đẩy đầy đủ lên firebase (đÃ THÊM hub)
        fb.pushTaskForDriver(
                req.getDriverId(),
                task.getId(),
                order.getId(),
                order.getTrackingCode(),
                displayAddress,
                displayLat,
                displayLng,
                legTypeForQuery,
                order.getPickupAddress(),
                toDouble(order.getPickupLat()),
                toDouble(order.getPickupLng()),
                order.getDropoffAddress(),
                toDouble(order.getDropoffLat()),
                toDouble(order.getDropoffLng()),
                null,       // receiverName -> DTO hiện không có
                null,       // receiverPhone -> DTO hiện không có
                hubAddress,
                hubLat,
                hubLng
        );

        // 7. báo sang order-service
        try {
            orderClient.transition(
                    order.getId(),
                    new OrderStatusChangeRequest("ASSIGNED", "dispatcher_assign"),
                    auth
            );
            orderClient.setAssignedDriver(order.getId(), req.getDriverId(), auth);
        } catch (Exception ex) {
            log.warn("assign(): order transition/assign-driver failed: {}", ex.getMessage());
        }

        return TaskSummaryResponse.builder()
                .taskId(task.getId())
                .legId(leg.getId())
                .legType(leg.getLegType())
                .roleHint("DELIVERY".equalsIgnoreCase(leg.getLegType()) ? "LAST_MILE_DRIVER" : "PICKUP_DRIVER")
                .trackingCode(order.getTrackingCode())
                .status(task.getStatus())
                .assignedAt(task.getAssignedAt())
                .build();
    }

    private int nextSeqNo(UUID orderId) {
        return legs.findByOrderIdOrderBySeqNoAsc(orderId).size() + 1;
    }

    private boolean isAssignable(String s) {
        return s == null
                || "PLANNED".equalsIgnoreCase(s)
                || "ASSIGNED".equalsIgnoreCase(s);
    }

    private int scoreStatusForReuse(String s) {
        if (s == null) return 0;
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "PLANNED" -> 0;
            case "ASSIGNED" -> 1;
            default -> 9;
        };
    }

    private Double toDouble(BigDecimal v) {
        return v != null ? v.doubleValue() : null;
    }
}

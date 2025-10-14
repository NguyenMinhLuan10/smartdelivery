// service/AssignService.java
package com.smartdelivery.shipment.service;

import com.smartdelivery.shipment.client.OrderClient;
import com.smartdelivery.shipment.dto.AssignRequest;
import com.smartdelivery.shipment.dto.OrderStatusChangeRequest;
import com.smartdelivery.shipment.dto.OrderTrackResponse;
import com.smartdelivery.shipment.dto.TaskSummaryResponse;
import com.smartdelivery.shipment.model.DriverTask;
import com.smartdelivery.shipment.model.Leg;
import com.smartdelivery.shipment.repository.DriverTaskRepository;
import com.smartdelivery.shipment.repository.LegRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j @Service @RequiredArgsConstructor
public class AssignService {

    private final LegRepository legs;
    private final DriverTaskRepository tasks;
    private final OrderClient orderClient;
    private final PlannerService planner;

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public TaskSummaryResponse assign(AssignRequest req, HttpServletRequest http) {
        final String auth = http.getHeader("Authorization");

        final OrderTrackResponse order = orderClient.getByTracking(req.getTrackingCode(), auth);
        if (order == null) throw new IllegalStateException("Order not found by tracking: " + req.getTrackingCode());

        final String requestedType = req.getType().toUpperCase(Locale.ROOT);
        String actualType = requestedType;
        if ("AUTO".equals(requestedType)) {
            var plan = planner.classify(order);
            actualType = planner.suggestLegTypeForAssignment(plan);
            log.info("assign(AUTO): classified={} -> legType={}", plan.kind(), actualType);
        }
        final String legTypeForQuery = actualType;

        final List<Leg> allLegs = legs.findByOrderIdOrderBySeqNoAsc(order.getId());
        Leg leg = allLegs.stream()
                .filter(l -> legTypeForQuery.equalsIgnoreCase(l.getLegType()))
                .sorted(Comparator.comparing((Leg l) -> scoreStatusForReuse(l.getStatus()))
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

        var task = DriverTask.builder()
                .legId(leg.getId())
                .orderId(order.getId())
                .trackingCode(order.getTrackingCode())
                .driverId(req.getDriverId())
                .status("ASSIGNED")
                .assignedAt(OffsetDateTime.now())
                .build();
        tasks.saveAndFlush(task);

        try {
            orderClient.transition(order.getId(),
                    new OrderStatusChangeRequest("ASSIGNED", "dispatcher_assign"), auth);
        } catch (Exception ex) {
            log.warn("assign(): order transition failed: {}", ex.getMessage());
        }

        return TaskSummaryResponse.builder()
                .taskId(task.getId()).legId(leg.getId())
                .legType(leg.getLegType())
                .roleHint("DELIVERY".equalsIgnoreCase(leg.getLegType()) ? "LAST_MILE_DRIVER" : "PICKUP_DRIVER")
                .trackingCode(order.getTrackingCode())
                .status(task.getStatus()).assignedAt(task.getAssignedAt()).build();
    }

    private int nextSeqNo(UUID orderId){ return legs.findByOrderIdOrderBySeqNoAsc(orderId).size() + 1; }
    private boolean isAssignable(String s){ return s==null || "PLANNED".equalsIgnoreCase(s) || "ASSIGNED".equalsIgnoreCase(s); }
    private int scoreStatusForReuse(String s){
        if (s==null) return 0;
        return switch (s.toUpperCase(Locale.ROOT)){
            case "PLANNED" -> 0;
            case "ASSIGNED" -> 1;
            default -> 9;
        };
    }
}

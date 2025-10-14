package com.smartdelivery.order.service;

import com.smartdelivery.order.dto.*;
import com.smartdelivery.order.model.Attachment;
import com.smartdelivery.order.model.Order;
import com.smartdelivery.order.model.TimelineEvent;
import com.smartdelivery.order.repository.AttachmentRepository;
import com.smartdelivery.order.repository.OrderRepository;
import com.smartdelivery.order.repository.TimelineEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orders;
    private final TimelineEventRepository events;
    private final AttachmentRepository attachments;

    private final TrackingCodeUtil tracking;
    private final PricingClient pricing;
    private final QrService qr;

    @Transactional
    public OrderDetailResponse create(Authentication auth, String authorizationHeader, CreateOrderRequest req){
        UUID customerId = UUID.fromString(auth.getName());

        // 1) gọi pricing
        Map<String,Object> body = Map.of(
                "serviceTypeCode", req.getServiceTypeCode(),
                "pickup",  Map.of("address", req.getPoints().getPickupAddress()),
                "dropoff", Map.of("address", req.getPoints().getDropoffAddress()),
                "weightKg", req.getWeightKg(),
                "volumetricWeightKg", req.getWeightKg()
        );
        var quoted = pricing.quote(body, authorizationHeader);

        // 2) tạo order
        Order o = new Order();
        o.setTrackingCode(tracking.gen());
        o.setCustomerUserId(customerId);
        o.setCustomerName(req.getCustomer().getName());
        o.setCustomerPhone(req.getCustomer().getPhone());
        o.setPickupFormattedAddr(req.getPoints().getPickupAddress());
        o.setDropoffFormattedAddr(req.getPoints().getDropoffAddress());
        o.setServiceTypeCode(req.getServiceTypeCode());
        o.setWeightKg(req.getWeightKg());
        o.setValueAmount(Optional.ofNullable(req.getValueAmount()).orElse(BigDecimal.ZERO));

        o.setPriceAmount(quoted.getPriceAmount());
        o.setPriceCurrency(quoted.getCurrency());
        o.setEtaPromisedAt(quoted.getEtaPromisedAt());

        // NEW: snapshot distance/time
        o.setDistanceKm(quoted.getDistanceKm());
        o.setTravelTimeMin(quoted.getTravelTimeMin());

        // geo/province để null giai đoạn này (sau này map-service set)
        o.setStatus("CREATED");
        o.setCreatedAt(OffsetDateTime.now());
        orders.save(o);

        // 3) QR
        String qrUrl = qr.generateAndSavePng(o.getTrackingCode());
        attachments.save(Attachment.builder()
                .entityType("ORDER").entityId(o.getId())
                .url(qrUrl).mime("image/png").build());

        // 4) timeline CREATED
        events.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_STATUS_CHANGED")
                .fromStatus(null).toStatus("CREATED").reason("customer_created").build());

        return toDetail(o, qrUrl, true);
    }

    public OrderDetailResponse getByTracking(String code){
        Order o = orders.findByTrackingCode(code).orElseThrow();
        String qrUrl = attachments.findByEntityTypeAndEntityIdOrderByCreatedAtAsc("ORDER", o.getId())
                .stream().filter(a -> a.getMime().contains("image")).findFirst().map(Attachment::getUrl).orElse(null);
        return toDetail(o, qrUrl, true);
    }

    @Transactional
    public void confirm(String trackingCode, ConfirmRequest req, Authentication auth){
        Order o = orders.findByTrackingCode(trackingCode).orElseThrow();
        if (!Objects.equals(o.getCustomerUserId(), UUID.fromString(auth.getName())))
            throw new IllegalStateException("Forbidden");
        if (!List.of("CREATED","CONFIRMED").contains(o.getStatus()))
            throw new IllegalStateException("Invalid state");
        String from = o.getStatus();
        o.setStatus("CONFIRMED");
        orders.save(o);
        events.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_STATUS_CHANGED").fromStatus(from).toStatus(o.getStatus())
                .reason("customer_confirm").build());
    }

    @Transactional
    public void cancel(String trackingCode, Authentication auth){
        Order o = orders.findByTrackingCode(trackingCode).orElseThrow();
        if (!Objects.equals(o.getCustomerUserId(), UUID.fromString(auth.getName())))
            throw new IllegalStateException("Forbidden");
        if (!List.of("CREATED","CONFIRMED").contains(o.getStatus()))
            throw new IllegalStateException("Only CREATED/CONFIRMED can be cancelled");
        String from = o.getStatus();
        o.setStatus("CANCELLED");
        orders.save(o);
        events.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_STATUS_CHANGED").fromStatus(from).toStatus("CANCELLED")
                .reason("customer_cancel").build());
    }

    @Transactional
    public void transition(UUID orderId, StatusChangeRequest req){
        Order o = orders.findById(orderId).orElseThrow();
        String from = o.getStatus();
        o.setStatus(req.getToStatus());
        orders.save(o);
        events.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_STATUS_CHANGED")
                .fromStatus(from).toStatus(req.getToStatus())
                .reason(req.getReason()).build());
    }

    public Page<OrderListItem> myOrders(UUID uid, Pageable pageable){
        return orders.findByCustomerUserId(uid, pageable).map(o ->
                OrderListItem.builder()
                        .id(o.getId()).trackingCode(o.getTrackingCode()).status(o.getStatus())
                        .priceAmount(o.getPriceAmount()).isCod(false)
                        .createdAt(o.getCreatedAt()).build());
    }

    private OrderDetailResponse toDetail(Order o, String qrUrl, boolean includeTimeline){
        var tl = includeTimeline
                ? events.findByEntityTypeAndEntityIdOrderByTsAsc("ORDER", o.getId())
                .stream().map(e -> TrackingTimelineEventDto.builder()
                        .type(e.getEventType()).fromStatus(e.getFromStatus()).toStatus(e.getToStatus())
                        .reason(e.getReason()).ts(e.getTs()).build()).toList()
                : List.<TrackingTimelineEventDto>of();

        return OrderDetailResponse.builder()
                .id(o.getId())
                .trackingCode(o.getTrackingCode())
                .status(o.getStatus())
                .serviceTypeCode(o.getServiceTypeCode())
                .pickupAddress(o.getPickupFormattedAddr())
                .dropoffAddress(o.getDropoffFormattedAddr())

                .distanceKm(o.getDistanceKm())
                .travelTimeMin(o.getTravelTimeMin())
                .pickupLat(o.getPickupLat())
                .pickupLng(o.getPickupLng())
                .dropoffLat(o.getDropoffLat())
                .dropoffLng(o.getDropoffLng())
                .pickupProvince(o.getPickupProvince())
                .dropoffProvince(o.getDropoffProvince())

                .priceAmount(o.getPriceAmount())
                .priceCurrency(o.getPriceCurrency())
                .etaPromisedAt(o.getEtaPromisedAt())
                .qrCodeUrl(qrUrl)
                .timeline(tl)
                .build();
    }
}

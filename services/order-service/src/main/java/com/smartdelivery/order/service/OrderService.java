package com.smartdelivery.order.service;

import com.smartdelivery.order.dto.*;
import com.smartdelivery.order.firebase.FirebaseGateway; // 👉 THÊM
import com.smartdelivery.order.model.Attachment;
import com.smartdelivery.order.model.Order;
import com.smartdelivery.order.model.OrderItem;
import com.smartdelivery.order.model.TimelineEvent;
import com.smartdelivery.order.repository.AttachmentRepository;
import com.smartdelivery.order.repository.OrderItemRepository;
import com.smartdelivery.order.repository.OrderRepository;
import com.smartdelivery.order.repository.TimelineEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final AttachmentRepository attachmentRepo;
    private final TimelineEventRepository eventRepo;

    private final PricingClient pricing;
    private final TrackingCodeUtil tcUtil;
    private final QrService qr;

    private final FirebaseGateway firebase; // 👉 THÊM

    @Transactional
    public OrderDetailResponse create(Authentication auth, String authHeader, CreateOrderRequest req) {

        // Call Pricing (an toàn null)
        Map<String,Object> pb = new HashMap<>();
        pb.put("serviceTypeCode", req.getServiceTypeCode());
        pb.put("pickupAddress", req.getPoints().getPickupAddress());
        pb.put("dropoffAddress", req.getPoints().getDropoffAddress());
        pb.put("weightKg", req.getWeightKg());
        if (req.getValueAmount() != null) pb.put("valueAmount", req.getValueAmount());

        PricingClient.QuoteResp quote = null;
        try { quote = pricing.quote(pb, authHeader); } catch (Exception ignored){}

        // Create Order
        Order o = new Order();
        o.setTrackingCode(tcUtil.gen());

        // snapshot người gửi
        o.setCustomerName(req.getCustomer().getName());
        o.setCustomerPhone(req.getCustomer().getPhone());
        if (auth != null) {
            try { o.setCustomerUserId(UUID.fromString(auth.getName())); } catch (Exception ignored) {}
        }

        // snapshot người nhận
        if (req.getReceiver() != null) {
            o.setReceiverName(req.getReceiver().getName());
            o.setReceiverPhone(req.getReceiver().getPhone());
        }

        // points (text)
        o.setPickupFormattedAddr(req.getPoints().getPickupAddress());
        o.setDropoffFormattedAddr(req.getPoints().getDropoffAddress());

        // 👇 THÊM: nếu FE gửi tọa độ thì lưu luôn
        if (req.getPoints().getPickupLat() != null && req.getPoints().getPickupLng() != null) {
            o.setPickupLat(BigDecimal.valueOf(req.getPoints().getPickupLat()));
            o.setPickupLng(BigDecimal.valueOf(req.getPoints().getPickupLng()));
        }
        if (req.getPoints().getDropoffLat() != null && req.getPoints().getDropoffLng() != null) {
            o.setDropoffLat(BigDecimal.valueOf(req.getPoints().getDropoffLat()));
            o.setDropoffLng(BigDecimal.valueOf(req.getPoints().getDropoffLng()));
        }

        // parcel/pricing
        o.setServiceTypeCode(req.getServiceTypeCode());
        o.setWeightKg(req.getWeightKg());
        o.setValueAmount(req.getValueAmount());

        if (quote != null) {
            if (quote.getPriceAmount() != null) o.setPriceAmount(quote.getPriceAmount());
            o.setPriceCurrency(quote.getCurrency());
            if (quote.getDistanceKm() != null) o.setDistanceKm(quote.getDistanceKm());
            if (quote.getTravelTimeMin() != null) o.setTravelTimeMin(quote.getTravelTimeMin());
            o.setEtaPromisedAt(quote.getEtaPromisedAt());
        }

        o.setStatus("CREATED");
        o = orderRepo.save(o);

        // Save items nếu có
        if (req.getGoods() != null && !req.getGoods().isEmpty()) {
            for (var g : req.getGoods()) {
                OrderItem it = OrderItem.builder()
                        .orderId(o.getId())
                        .name(g.getName())
                        .qty(g.getQty())
                        .weightKg(BigDecimal.valueOf(g.getWeightKg()))
                        .valueAmount(BigDecimal.valueOf(g.getValue()))
                        .build();
                orderItemRepo.save(it);
            }
        }

        // QR
        String qrUrl = qr.generateAndSavePng(o.getTrackingCode());
        attachmentRepo.save(Attachment.builder()
                .entityType("ORDER").entityId(o.getId())
                .url(qrUrl).mime("image/png").build());

        // timeline
        eventRepo.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_CREATED")
                .toStatus("CREATED")
                .ts(OffsetDateTime.now())
                .build());

        // 👉 THÊM: đẩy lên Firebase ngay lúc tạo
        firebase.upsertOrder(
                o.getId(),
                o.getTrackingCode(),
                o.getStatus(),
                o.toGeoMap(),
                o.getAssignedDriverId() // lúc tạo thường null
        );

        return toDetail(o, qrUrl);
    }

    public Page<OrderListItem> myOrders(UUID userId, Pageable pageable){
        return orderRepo.findByCustomerUserId(userId, pageable)
                .map(o -> OrderListItem.builder()
                        .id(o.getId())
                        .trackingCode(o.getTrackingCode())
                        .status(o.getStatus())
                        .priceAmount(o.getPriceAmount() == null ? BigDecimal.ZERO : o.getPriceAmount())
                        .isCod(Boolean.FALSE)
                        .createdAt(o.getCreatedAt())
                        .build());
    }

    public OrderDetailResponse getByTracking(String code){
        Order o = orderRepo.findByTrackingCode(code)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        String qrUrl = attachmentRepo.findByEntityTypeAndEntityIdOrderByCreatedAtAsc("ORDER", o.getId())
                .stream().findFirst().map(Attachment::getUrl).orElse(null);
        return toDetail(o, qrUrl);
    }

    public void confirm(String trackingCode, ConfirmRequest req, Authentication auth){
        Order o = orderRepo.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        String from = o.getStatus();
        o.setStatus("CONFIRMED");
        orderRepo.save(o);

        eventRepo.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_STATUS_CHANGED")
                .fromStatus(from).toStatus("CONFIRMED")
                .reason("method=" + req.getMethod())
                .build());
    }

    public void cancel(String trackingCode, Authentication auth){
        Order o = orderRepo.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        String from = o.getStatus();
        o.setStatus("CANCELLED");
        orderRepo.save(o);

        eventRepo.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_STATUS_CHANGED")
                .fromStatus(from).toStatus("CANCELLED")
                .reason("user_cancel")
                .build());
    }

    @Transactional
    public Order transition(UUID id, StatusChangeRequest req){
        Order o = orderRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        String from = o.getStatus();
        o.setStatus(req.getToStatus());
        orderRepo.save(o);

        eventRepo.save(TimelineEvent.builder()
                .entityType("ORDER").entityId(o.getId())
                .eventType("ORDER_STATUS_CHANGED")
                .fromStatus(from).toStatus(req.getToStatus())
                .reason(req.getReason())
                .build());

        // 👉 tùy bạn: nếu muốn mỗi lần đổi status cũng đẩy Firebase thì thêm dòng này
        firebase.upsertOrder(
                o.getId(),
                o.getTrackingCode(),
                o.getStatus(),
                o.toGeoMap(),
                o.getAssignedDriverId()
        );

        return o;
    }

    // 👇 THÊM: để internal controller gán driver
    @Transactional
    public Order assignDriver(UUID id, UUID driverId) {
        Order o = orderRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found"));
        o.setAssignedDriverId(driverId);
        orderRepo.save(o);

        // 👉 gán driver xong thì cũng đẩy lại để FE track dùng đúng driver
        firebase.upsertOrder(
                o.getId(),
                o.getTrackingCode(),
                o.getStatus(),
                o.toGeoMap(),
                o.getAssignedDriverId()
        );

        return o;
    }

    private OrderDetailResponse toDetail(Order o, String qrUrl) {
        var evs = eventRepo.findByEntityTypeAndEntityIdOrderByTsAsc("ORDER", o.getId());
        var timeline = evs.stream().map(e -> TrackingTimelineEventDto.builder()
                .type(e.getEventType())
                .fromStatus(e.getFromStatus())
                .toStatus(e.getToStatus())
                .reason(e.getReason())
                .ts(e.getTs())
                .build()).toList();

        var items = orderItemRepo.findByOrderId(o.getId()).stream().map(it ->
                OrderItemDto.builder()
                        .desc(it.getName())
                        .qty(it.getQty())
                        .weight(it.getWeightKg() != null ? it.getWeightKg().doubleValue() : null)
                        .value(it.getValueAmount() != null ? it.getValueAmount().doubleValue() : null)
                        .build()
        ).toList();

        return OrderDetailResponse.builder()
                .id(o.getId())
                .trackingCode(o.getTrackingCode())
                .status(o.getStatus())
                .serviceTypeCode(o.getServiceTypeCode())
                .pickupAddress(nz(o.getPickupFormattedAddr()))
                .dropoffAddress(nz(o.getDropoffFormattedAddr()))
                .pickupLat(o.getPickupLat() != null ? o.getPickupLat().doubleValue() : null)
                .pickupLng(o.getPickupLng() != null ? o.getPickupLng().doubleValue() : null)
                .dropoffLat(o.getDropoffLat() != null ? o.getDropoffLat().doubleValue() : null)
                .dropoffLng(o.getDropoffLng() != null ? o.getDropoffLng().doubleValue() : null)
                .receiverName(nz(o.getReceiverName()))
                .receiverPhone(nz(o.getReceiverPhone()))
                .distanceKm(o.getDistanceKm() != null ? o.getDistanceKm().doubleValue() : 0.0d)
                .travelTimeMin(o.getTravelTimeMin() != null ? o.getTravelTimeMin() : 0)
                .priceAmount(o.getPriceAmount() != null ? o.getPriceAmount() : BigDecimal.ZERO)
                .priceCurrency(nz(o.getPriceCurrency()))
                .isCod(Boolean.FALSE)
                .codAmount(BigDecimal.ZERO)
                .etaPromisedAt(o.getEtaPromisedAt())
                .timeline(timeline)
                .qrCodeUrl(qrUrl)
                .items(items)
                .assignedDriverId(o.getAssignedDriverId())
                .build();
    }

    private static String nz(String s){ return s == null ? "" : s; }
}

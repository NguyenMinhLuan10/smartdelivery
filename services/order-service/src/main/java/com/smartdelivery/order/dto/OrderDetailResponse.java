package com.smartdelivery.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class OrderDetailResponse {
    private UUID id;
    private String trackingCode;

    private String status;
    private String serviceTypeCode;

    private String pickupAddress;
    private String dropoffAddress;

    // Người nhận (snapshot)
    private String receiverName;
    private String receiverPhone;

    // từ Pricing snapshot
    private Double distanceKm;      // Double để FE parse dễ
    private Integer travelTimeMin;

    private BigDecimal priceAmount;
    private String priceCurrency;

    private Boolean isCod;
    private BigDecimal codAmount;

    private OffsetDateTime etaPromisedAt;

    private List<TrackingTimelineEventDto> timeline;

    private String qrCodeUrl; // images/qrcodes/<tracking>.png

    // Items (tuỳ chọn – hiển thị)
    private List<OrderItemDto> items;
}

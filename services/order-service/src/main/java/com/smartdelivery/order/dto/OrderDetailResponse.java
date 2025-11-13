package com.smartdelivery.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderDetailResponse {
    private UUID id;
    private String trackingCode;

    private String status;
    private String serviceTypeCode;

    private String pickupAddress;
    private String dropoffAddress;

    // toạ độ
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;

    // người nhận
    private String receiverName;
    private String receiverPhone;

    // pricing snapshot
    private Double distanceKm;
    private Integer travelTimeMin;

    private BigDecimal priceAmount;
    private String priceCurrency;

    private Boolean isCod;
    private BigDecimal codAmount;

    private OffsetDateTime etaPromisedAt;

    private List<TrackingTimelineEventDto> timeline;
    private String qrCodeUrl;
    private List<OrderItemDto> items;

    // 👇 QUAN TRỌNG: để Flutter lấy driverId
    private UUID assignedDriverId;
}

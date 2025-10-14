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

    // NEW
    private BigDecimal distanceKm;
    private Integer travelTimeMin;

    // NEW (optional – có thể null)
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private BigDecimal dropoffLat;
    private BigDecimal dropoffLng;
    private String pickupProvince;
    private String dropoffProvince;

    private BigDecimal priceAmount;
    private String priceCurrency;

    private Boolean isCod;
    private BigDecimal codAmount;

    private OffsetDateTime etaPromisedAt;

    private List<TrackingTimelineEventDto> timeline;

    private String qrCodeUrl; // images/qrcodes/<tracking>.png
}

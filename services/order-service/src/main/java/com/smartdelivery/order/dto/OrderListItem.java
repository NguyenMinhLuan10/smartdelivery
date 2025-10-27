package com.smartdelivery.order.dto;

import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.OffsetDateTime; import java.util.UUID;

@Data @Builder
public class OrderListItem {
    private UUID id;
    private String trackingCode;
    private String status;
    private BigDecimal priceAmount; // BigDecimal để khớp DB
    private Boolean isCod;
    private OffsetDateTime createdAt;
}

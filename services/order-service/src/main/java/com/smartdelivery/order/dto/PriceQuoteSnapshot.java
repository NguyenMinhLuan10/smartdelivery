// dto/PriceQuoteSnapshot.java
package com.smartdelivery.order.dto;

import lombok.Builder; import lombok.Data;
import java.math.BigDecimal; import java.time.OffsetDateTime;

@Data @Builder
public class PriceQuoteSnapshot {
    private BigDecimal distanceKm;
    private Integer travelTimeMin;
    private BigDecimal priceAmount;
    private String priceCurrency;
    private OffsetDateTime etaPromisedAt;
}

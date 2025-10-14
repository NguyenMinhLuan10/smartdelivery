// dto/OrderTrackResponse.java
package com.smartdelivery.shipment.dto;
import lombok.Data;
import java.math.BigDecimal; import java.util.UUID;
@Data
public class OrderTrackResponse {
    private UUID id;
    private String trackingCode;
    private String status;
    private String serviceTypeCode;
    private String pickupAddress;
    private String dropoffAddress;
    private BigDecimal distanceKm;
    private Integer travelTimeMin;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private BigDecimal dropoffLat;
    private BigDecimal dropoffLng;
    private String pickupProvince;
    private String dropoffProvince;
}

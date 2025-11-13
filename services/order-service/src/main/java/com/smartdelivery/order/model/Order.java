package com.smartdelivery.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name="orders",
        indexes = {
                @Index(name="idx_orders_status_updated", columnList="status,updated_at"),
                @Index(name="idx_orders_created_at", columnList="created_at"),
                @Index(name="idx_orders_customer_created", columnList="customer_user_id,created_at")
        })
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Order {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @Column(unique = true)
    private String trackingCode;

    private UUID customerUserId;
    private String customerName;
    private String customerPhone;

    private String receiverName;
    private String receiverPhone;

    private String pickupFormattedAddr;
    private String dropoffFormattedAddr;

    // geo
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private BigDecimal dropoffLat;
    private BigDecimal dropoffLng;
    private String pickupProvince;
    private String dropoffProvince;

    private String serviceTypeCode;

    private BigDecimal weightKg;
    private BigDecimal valueAmount;

    private BigDecimal priceAmount;
    private String priceCurrency;

    private BigDecimal distanceKm;
    private Integer travelTimeMin;

    private String status;
    private OffsetDateTime etaPromisedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // driver được gán
    @Column(name = "assigned_driver_id")
    private UUID assignedDriverId;

    @PrePersist void preInsert(){ createdAt = OffsetDateTime.now(); }
    @PreUpdate  void preUpdate(){ updatedAt = OffsetDateTime.now(); }

    public Map<String,Object> toGeoMap() {
        Map<String,Object> m = new HashMap<>();
        if (pickupLat != null && pickupLng != null) {
            m.put("pickupLat", pickupLat.doubleValue());
            m.put("pickupLng", pickupLng.doubleValue());
        }
        if (dropoffLat != null && dropoffLng != null) {
            m.put("dropoffLat", dropoffLat.doubleValue());
            m.put("dropoffLng", dropoffLng.doubleValue());
        }
        if (pickupProvince != null)  m.put("pickupProvince", pickupProvince);
        if (dropoffProvince != null) m.put("dropoffProvince", dropoffProvince);
        return m.isEmpty() ? null : m;
    }
}

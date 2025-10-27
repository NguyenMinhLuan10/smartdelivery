package com.smartdelivery.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    // customer snapshot
    private UUID customerUserId;
    private String customerName;
    private String customerPhone;

    // ➕ receiver snapshot
    private String receiverName;
    private String receiverPhone;

    // points (text + optional geo fields)
    private String pickupFormattedAddr;
    private String dropoffFormattedAddr;

    // optional geo (reserve cho map-service)
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    private BigDecimal dropoffLat;
    private BigDecimal dropoffLng;
    private String pickupProvince;
    private String dropoffProvince;

    private String serviceTypeCode;

    // parcel
    private BigDecimal weightKg;
    private BigDecimal valueAmount;

    // pricing snapshot
    private BigDecimal priceAmount;
    private String priceCurrency;

    // NEW: snapshot từ Pricing
    private BigDecimal distanceKm;   // vd 12.35
    private Integer travelTimeMin;   // vd 46

    // lifecycle
    private String status;                 // CREATED/.../DELIVERED/FAILED/CANCELLED
    private OffsetDateTime etaPromisedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @PrePersist void preInsert(){ createdAt = OffsetDateTime.now(); }
    @PreUpdate  void preUpdate(){ updatedAt = OffsetDateTime.now(); }
}

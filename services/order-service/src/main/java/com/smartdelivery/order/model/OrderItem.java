package com.smartdelivery.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_items", indexes = {
        @Index(name="idx_order_items_order", columnList="order_id")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItem {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    @Column(name="order_id", nullable=false)
    private UUID orderId;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false)
    private Integer qty;

    @Column(name="weight_kg", precision=10, scale=3)
    private BigDecimal weightKg;

    @Column(name="value_amount", precision=18, scale=2)
    private BigDecimal valueAmount;

    private OffsetDateTime createdAt;

    @PrePersist void preInsert(){ createdAt = OffsetDateTime.now(); }
}

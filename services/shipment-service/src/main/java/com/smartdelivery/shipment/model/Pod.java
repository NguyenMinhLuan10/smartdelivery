package com.smartdelivery.shipment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="pods",
        indexes = {@Index(name="idx_pods_task", columnList="taskId"),
                @Index(name="idx_pods_created_at", columnList="createdAt")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pod {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    private UUID taskId;
    private String method;  // OTP/QR/PHOTO
    private String otpHash;
    private String photoUrl;
    private String signedName;
    private String signedPhone;
    private BigDecimal collectedCodAmount;

    private Double podLat;
    private Double podLng;

    private OffsetDateTime createdAt;
    @PrePersist void pre(){ createdAt = OffsetDateTime.now(); }
}

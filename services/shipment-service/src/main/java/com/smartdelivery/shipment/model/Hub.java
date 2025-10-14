package com.smartdelivery.shipment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="hubs", indexes=@Index(name="idx_hub_code", columnList="code", unique=true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hub {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;
    @Column(unique = true) private String code; // HCM-ORIGIN / HN-DEST ...
    private String name;
    private String addressText;
    private Double lat;
    private Double lng;
    private OffsetDateTime createdAt;
    @PrePersist void pre(){ createdAt = OffsetDateTime.now(); }
}

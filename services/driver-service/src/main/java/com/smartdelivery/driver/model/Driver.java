package com.smartdelivery.driver.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="drivers",
        indexes = @Index(name="idx_drivers_online_seen", columnList="onlineStatus,lastSeenAt"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Driver {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    private UUID userId;
    private String homeHubCode;
    private Boolean active;
    private String onlineStatus;  // ONLINE/OFFLINE/BUSY

    private Double lastLat;
    private Double lastLng;
    private OffsetDateTime lastSeenAt;
    private OffsetDateTime createdAt;

    @PrePersist void pre(){ createdAt = OffsetDateTime.now(); }
}

package com.smartdelivery.identity.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name="idx_rt_user", columnList = "user_id"),
        @Index(name="idx_rt_exp", columnList = "expires_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="user_id", nullable = false)
    private UUID userId;

    @Column(length = 512, nullable = false)
    private String token;     // giá trị refresh trả cho client

    @Column(name="issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name="expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name="revoked_at")
    private Instant revokedAt;

    public boolean isActive(Instant now){
        return revokedAt == null && expiresAt.isAfter(now);
    }
}

package com.smartdelivery.identity.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="otp_tokens", indexes = {
        @Index(name="idx_otp_user_kind_exp", columnList="user_id, kind, expires_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OtpToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Soft-link -> users.id
    @Column(name="user_id", nullable = false)
    private UUID userId;

    // REGISTER/RESET/2FA
    @Column(length = 16, nullable = false)
    private String kind;

    @Column(name="otp_hash", length = 255) private String otpHash;
    @Column(name="expires_at") private Instant expiresAt;
    @Column(name="used_at")    private Instant usedAt;
    @Column(name="created_at") private Instant createdAt = Instant.now();
}

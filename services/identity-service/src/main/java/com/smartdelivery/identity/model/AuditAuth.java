package com.smartdelivery.identity.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="audit_logs_auth", indexes = {
        @Index(name="idx_audit_actor_ts", columnList = "actor_user_id, ts"),
        @Index(name="idx_audit_target", columnList = "target, target_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditAuth {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="actor_user_id") private UUID actorUserId; // soft-link users.id
    @Column(length = 64) private String action;  // LOGIN/REGISTER/UPDATE/...
    @Column(length = 64) private String target;  // USER/ROLE/OTP/...
    @Column(name="target_id") private UUID targetId;

    @Column(name="ts") private Instant ts = Instant.now();

    @Lob @Column(name="meta") private String metaJson; // thay jsonb bằng text (MySQL)
}

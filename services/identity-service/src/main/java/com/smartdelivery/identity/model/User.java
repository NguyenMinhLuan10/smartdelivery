package com.smartdelivery.identity.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_phone", columnList = "phone"),
        @Index(name = "idx_users_status", columnList = "status"),
        @Index(name = "idx_users_role_code", columnList = "role_code")
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // 🔴 CỘT PHỤC VỤ AUTH/FILTER NHANH
    @Column(name = "role_code", length = 32, nullable = false)
    private String roleCode; // ADMIN/CUSTOMER/DRIVER/DISPATCHER

    @Column(unique = true, length = 255)
    private String email;

    @Column(unique = true, length = 32)
    private String phone;

    @Column(length = 128)
    private String name;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(length = 16)
    private String status = "ACTIVE";

    @Column(name = "twofa_enabled")
    private Boolean twofaEnabled = false;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(length = 8)
    private String locale;

    @Column(length = 64)
    private String tz;

    // 🔗 BẢN GHI CHUẨN HOÁ (metadata)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // ------- Đồng bộ hoá 2 trường -------
    public void setRole(Role role) {
        this.role = role;
        if (role != null && role.getCode() != null) {
            this.roleCode = role.getCode().toUpperCase();
        }
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = (roleCode == null) ? null : roleCode.toUpperCase();
        // KHÔNG tự load Role ở setter để tránh N+1; dịch vụ sẽ gán Role khi cần
    }
}

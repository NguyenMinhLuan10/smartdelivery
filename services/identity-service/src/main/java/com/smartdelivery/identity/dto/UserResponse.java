package com.smartdelivery.identity.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class UserResponse {
    private UUID id;
    private String roleCode;
    private String email;
    private String phone;
    private String name;
    private String status;
    private Instant createdAt;
}

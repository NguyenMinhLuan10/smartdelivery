package com.smartdelivery.identity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class TokenPairResponse {
    private String accessToken;
    private long   accessExpiresIn;
    private String refreshToken;
    private long   refreshExpiresIn;
}

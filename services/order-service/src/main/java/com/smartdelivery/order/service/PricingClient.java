// service/PricingClient.java
package com.smartdelivery.order.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class PricingClient {

    @Value("${app.services.pricing-base}")
    private String pricingBase;

    private final RestTemplate rt = new RestTemplate();

    @Data @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuoteResp {
        private BigDecimal distanceKm;
        private Integer travelTimeMin;
        private BigDecimal priceAmount;
        private String currency;
        private String serviceTypeCode;
        private OffsetDateTime etaPromisedAt;
    }

    public QuoteResp quote(Map<String,Object> body, String authHeader){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authHeader != null) headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        var entity = new HttpEntity<>(body, headers);
        return rt.postForEntity(pricingBase + "/quote", entity, QuoteResp.class).getBody();
    }
}

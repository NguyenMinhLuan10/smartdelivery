package com.smartdelivery.shipment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientsConfig {

    // Gọi QUA GATEWAY (khuyến nghị dev): http://localhost:8085
    // Nếu muốn gọi trực tiếp service discovery: set thành http://driver-service
    @Bean
    public RestClient driverRestClient(
            @Value("${app.driver.base-url:http://localhost:8085}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl) // VD: http://localhost:8085
                .build();
    }
}

// client/GwClient.java  (gọi qua API Gateway)
package com.smartdelivery.dispatch.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component @RequiredArgsConstructor
public class GwClient {
    private final RestTemplate rt;
    @Value("${app.gateway.base-url:http://localhost:8085}") private String gw;

    public <T> T get(String path, Class<T> type, String auth){
        HttpHeaders h = new HttpHeaders();
        if (auth!=null) h.setBearerAuth(extract(auth));
        return rt.exchange(gw+path, HttpMethod.GET, new HttpEntity<>(h), type).getBody();
    }
    public <T> T post(String path, Object body, Class<T> type, String auth){
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_JSON);
        if (auth!=null) h.setBearerAuth(extract(auth));
        return rt.exchange(gw+path, HttpMethod.POST, new HttpEntity<>(body, h), type).getBody();
    }

    private String extract(String auth){ return auth.startsWith("Bearer ") ? auth.substring(7) : auth; }
}

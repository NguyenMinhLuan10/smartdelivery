package com.smartdelivery.driver.controller;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/drivers/location")
public class DriverLocationController {

    private final RestTemplate rest = new RestTemplate();

    // lấy từ application.yml bạn gửi
    @Value("${app.firebase.db-url}")
    private String dbUrl;

    @Value("${app.firebase.credentials}")
    private String credPath;

    @GetMapping("/{driverId}")
    public DriverLocationDto get(@PathVariable String driverId) throws Exception {
        // 1. kiểm tra cấu hình
        if (dbUrl == null || dbUrl.isBlank()) {
            throw new IllegalStateException("Missing app.firebase.db-url / FIREBASE_DB_URL");
        }
        if (credPath == null || credPath.isBlank()) {
            throw new IllegalStateException("Missing app.firebase.credentials / GOOGLE_APPLICATION_CREDENTIALS");
        }

        // 2. lấy access token từ file service account (KHÔNG dùng getApplicationDefault())
        GoogleCredentials cred = GoogleCredentials
                .fromStream(new FileInputStream(credPath))
                .createScoped(List.of(
                        "https://www.googleapis.com/auth/firebase.database",
                        "https://www.googleapis.com/auth/userinfo.email",
                        "https://www.googleapis.com/auth/cloud-platform"
                ));
        cred.refreshIfExpired();
        String token = cred.getAccessToken().getTokenValue();

        // 3. gọi REST tới Realtime Database
        String base = dbUrl.endsWith("/") ? dbUrl : dbUrl + "/";
        String url = base + "drivers/" + driverId + ".json?access_token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        ResponseEntity<Map> res = rest.exchange(url, HttpMethod.GET, null, Map.class);
        Map<String, Object> data = res.getBody();

        if (data == null) {
            return new DriverLocationDto(driverId, null, null, false, null);
        }

        Double lat = data.get("lastLat") != null ? ((Number) data.get("lastLat")).doubleValue() : null;
        Double lng = data.get("lastLng") != null ? ((Number) data.get("lastLng")).doubleValue() : null;
        Boolean online = data.get("online") != null ? (Boolean) data.get("online") : Boolean.FALSE;
        String lastSeenAt = data.get("lastSeenAt") != null ? data.get("lastSeenAt").toString() : null;

        return new DriverLocationDto(driverId, lat, lng, online, lastSeenAt);
    }

    @Data
    @AllArgsConstructor
    public static class DriverLocationDto {
        private String driverId;
        private Double lat;
        private Double lng;
        private Boolean online;
        private String lastSeenAt;
    }
}

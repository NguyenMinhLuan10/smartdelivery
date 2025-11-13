package com.smartdelivery.driver.dev;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.database.DatabaseReference;
import com.google.common.util.concurrent.UncheckedExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/dev/firebase")
@RequiredArgsConstructor
public class FirebaseDevController {

    private final DatabaseReference root;
    private final RestTemplate rest = new RestTemplate();

    @Value("${app.firebase.db-url}")
    private String dbUrl;

    @Value("${app.firebase.credentials:${GOOGLE_APPLICATION_CREDENTIALS:}}")
    private String credPath;

    @Value("${spring.application.name:unknown}")
    private String appName;

    /** Xem nhanh cấu hình (không lộ secret) */
    @GetMapping("/info")
    public Map<String,Object> info() {
        String safeCred = credPath == null ? null
                : credPath.replaceAll("(?s).*(.smartdelivery-secrets/|/)([^/]+)$", ".../$2");
        return Map.of(
                "app", appName,
                "dbUrl", dbUrl,
                "credentialsPath", safeCred
        );
    }

    /** Ghi bằng Admin SDK, tăng timeout lên 20s và trả lỗi rõ ràng nếu treo. */
    @PostMapping("/ping")
    public ResponseEntity<?> ping() {
        String node = "diagnostics";
        String key = UUID.randomUUID().toString();
        Map<String, Object> payload = new HashMap<>();
        try {
            payload.put("service", appName);
            payload.put("ts", Instant.now().toString());
            payload.put("host", InetAddress.getLocalHost().getHostName());

            root.child(node).child(key).setValueAsync(payload).get(20, TimeUnit.SECONDS);

            return ResponseEntity.ok(Map.of(
                    "method", "admin-sdk",
                    "wrote", true,
                    "path", "/" + node + "/" + key,
                    "payload", payload
            ));
        } catch (java.util.concurrent.TimeoutException te) {
            return ResponseEntity.status(504).body(Map.of(
                    "method", "admin-sdk",
                    "wrote", false,
                    "error", "Timeout waiting for Firebase Admin SDK write (>20s)",
                    "hints", List.of(
                            "Kiểm tra app.firebase.db-url đúng dạng https://<project>-default-rtdb.<region>.firebasedatabase.app",
                            "Đảm bảo máy có thể ra Internet (TCP 443) và không bị proxy chặn",
                            "Service account có quyền Realtime Database",
                            "Đồng hồ hệ thống đúng giờ (TLS/Token lệch giờ sẽ lỗi)"
                    )
            ));
        } catch (Exception ex) {
            String msg = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "method", "admin-sdk",
                    "wrote", false,
                    "error", msg
            ));
        }
    }

    /** Ghi bằng REST (PUT) dùng access_token từ service account — giúp phân biệt lỗi mạng/SDK. */
    @PostMapping("/ping-rest")
    public ResponseEntity<?> pingRest() {
        try {
            String key = UUID.randomUUID().toString();
            Map<String,Object> payload = Map.of(
                    "service", appName,
                    "ts", Instant.now().toString(),
                    "note", "ping via REST"
            );

            GoogleCredentials cred = GoogleCredentials.getApplicationDefault().createScoped(List.of(
                    "https://www.googleapis.com/auth/firebase.database",
                    "https://www.googleapis.com/auth/userinfo.email",
                    "https://www.googleapis.com/auth/cloud-platform"
            ));
            cred.refreshIfExpired();
            String token = cred.getAccessToken().getTokenValue();

            String base = dbUrl.endsWith("/") ? dbUrl : dbUrl + "/";
            String url = base + "diagnostics/" + key + ".json?access_token=" +
                    URLEncoder.encode(token, StandardCharsets.UTF_8);

            ResponseEntity<String> res = rest.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload), String.class);

            return ResponseEntity.ok(Map.of(
                    "method", "rest-put",
                    "status", res.getStatusCodeValue(),
                    "path", "/diagnostics/" + key,
                    "echo", res.getBody()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "method", "rest-put",
                    "error", ex.getClass().getSimpleName() + ": " + ex.getMessage()
            ));
        }
    }

    /** Đọc record gần nhất qua REST (không phải Admin SDK). */
    @GetMapping("/read-latest")
    public ResponseEntity<?> readLatest() {
        try {
            GoogleCredentials cred = GoogleCredentials.getApplicationDefault().createScoped(List.of(
                    "https://www.googleapis.com/auth/firebase.database",
                    "https://www.googleapis.com/auth/userinfo.email",
                    "https://www.googleapis.com/auth/cloud-platform"
            ));
            cred.refreshIfExpired();
            String token = cred.getAccessToken().getTokenValue();

            String base = dbUrl.endsWith("/") ? dbUrl : dbUrl + "/";
            String url = base + "diagnostics.json?orderBy=%22$key%22&limitToLast=1&print=pretty&access_token="
                    + URLEncoder.encode(token, StandardCharsets.UTF_8);

            ResponseEntity<String> res = rest.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, String.class);

            return ResponseEntity.ok(Map.of(
                    "url", url.replace(token, "<token>"),
                    "status", res.getStatusCodeValue(),
                    "body", res.getBody()
            ));
        } catch (Exception ex) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", ex.getClass().getSimpleName() + ": " + ex.getMessage()
            ));
        }
    }
}

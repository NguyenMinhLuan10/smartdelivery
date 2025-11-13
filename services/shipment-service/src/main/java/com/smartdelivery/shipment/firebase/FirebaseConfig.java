package com.smartdelivery.shipment.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

import java.io.FileInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class FirebaseConfig {
    @Value("${app.firebase.db-url:${FIREBASE_DB_URL:}}") private String dbUrl;
    @Value("${app.firebase.credentials:${GOOGLE_APPLICATION_CREDENTIALS:}}") private String cred;
    @PostConstruct public void init() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) return;
        try (FileInputStream in = new FileInputStream(cred)) {
            FirebaseOptions opts = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .setDatabaseUrl(dbUrl).build();
            FirebaseApp.initializeApp(opts);
        }
    }
    @Bean public DatabaseReference firebaseRoot(){ return FirebaseDatabase.getInstance().getReference(); }
}

package com.smartdelivery.order.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
public class FirebaseConfig {

    @Value("${app.firebase.credentials:${GOOGLE_APPLICATION_CREDENTIALS:}}")
    private String serviceAccountPath;

    @Value("${app.firebase.db-url:${FIREBASE_DB_URL:}}")
    private String databaseUrl;

    @Bean
    public FirebaseApp firebaseApp() throws Exception {
        var p = Paths.get(serviceAccountPath).toAbsolutePath();
        if (!Files.exists(p)) {
            throw new IllegalStateException("Service account file NOT FOUND at: " + p);
        }

        try (var in = new FileInputStream(p.toFile())) {
            var options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .setDatabaseUrl(databaseUrl)
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        }
    }

    @Bean(name = "firebaseRoot")
    public DatabaseReference firebaseRoot(FirebaseApp app) {
        return FirebaseDatabase.getInstance(app).getReference();
    }
}

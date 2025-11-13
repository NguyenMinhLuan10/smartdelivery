package com.smartdelivery.driver.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.util.List;

@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.db-url:${FIREBASE_DB_URL:}}")
    private String dbUrl;

    @Value("${app.firebase.credentials:${GOOGLE_APPLICATION_CREDENTIALS:}}")
    private String credPath;

    @PostConstruct
    public void init() throws Exception {
        if (FirebaseApp.getApps() != null && !FirebaseApp.getApps().isEmpty()) return;
        if (credPath == null || credPath.isBlank())
            throw new IllegalStateException("Missing GOOGLE_APPLICATION_CREDENTIALS/app.firebase.credentials");
        if (dbUrl == null || dbUrl.isBlank())
            throw new IllegalStateException("Missing FIREBASE_DB_URL/app.firebase.db-url");

        try (FileInputStream serviceAccount = new FileInputStream(credPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)
                            .createScoped(List.of("https://www.googleapis.com/auth/firebase.database",
                                    "https://www.googleapis.com/auth/userinfo.email",
                                    "https://www.googleapis.com/auth/cloud-platform")))
                    .setDatabaseUrl(dbUrl)
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }

    @Bean
    public DatabaseReference firebaseRoot() {
        return FirebaseDatabase.getInstance().getReference();
    }
}

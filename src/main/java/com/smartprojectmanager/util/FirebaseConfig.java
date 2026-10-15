package com.smartprojectmanager.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.FileInputStream;
import java.io.InputStream;

@WebListener
public class FirebaseConfig implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                // In production, use an environment variable to point to the service account key
                String serviceAccountPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
                
                InputStream serviceAccount;
                if (serviceAccountPath != null && !serviceAccountPath.isEmpty()) {
                    serviceAccount = new FileInputStream(serviceAccountPath);
                } else {
                    // Fallback to a file bundled in WEB-INF/classes (if provided)
                    // The developer MUST place the JSON file securely and NEVER commit it to git
                    serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                }

                if (serviceAccount == null) {
                    System.err.println("WARNING: Firebase service account configuration missing. Firebase Auth will fail.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase successfully initialized on server startup.");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize Firebase.");
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Cleanup if needed
    }
}

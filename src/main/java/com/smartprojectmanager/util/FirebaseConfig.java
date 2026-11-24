package com.smartprojectmanager.util;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebListener;

@WebListener
public class FirebaseConfig implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent event) {
    if (Settings.demo() || !FirebaseApp.getApps().isEmpty()) return;
    String path = Settings.get("FIREBASE_SERVICE_ACCOUNT_PATH", "");
    if (path.isEmpty()) {
      event
          .getServletContext()
          .log("Set FIREBASE_SERVICE_ACCOUNT_PATH to enable Firebase sign-in.");
      return;
    }
    try (InputStream in = new FileInputStream(path)) {
      FirebaseApp.initializeApp(
          FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build());
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot initialize Firebase credentials", ex);
    }
  }
}

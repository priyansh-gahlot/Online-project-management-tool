package com.smartprojectmanager.util;

public final class Settings {
  private Settings() {}

  public static String get(String key, String fallback) {
    String v = System.getProperty(key, System.getenv(key));
    return v == null || v.isBlank() ? fallback : v;
  }

  public static boolean demo() {
    return Boolean.parseBoolean(get("APP_DEMO", "false"));
  }
}

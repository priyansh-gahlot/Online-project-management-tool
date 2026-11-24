package com.smartprojectmanager.dao;

import com.smartprojectmanager.util.Settings;
import java.sql.*;

public final class DBConnection {
  private DBConnection() {}

  public static Connection getConnection() throws SQLException {
    String url = Settings.get("DB_URL", "jdbc:h2:./data/smartpm;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
    if (Settings.demo() && !url.startsWith("jdbc:h2:"))
      throw new SQLException("Demo mode requires an isolated H2 database.");
    return DriverManager.getConnection(
        url,
        Settings.get("DB_USER", url.startsWith("jdbc:h2:") ? "sa" : "root"),
        Settings.get("DB_PASSWORD", ""));
  }
}

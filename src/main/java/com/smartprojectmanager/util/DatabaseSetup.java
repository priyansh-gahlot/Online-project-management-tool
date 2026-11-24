package com.smartprojectmanager.util;

import com.smartprojectmanager.dao.DBConnection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import javax.servlet.*;
import javax.servlet.annotation.WebListener;

@WebListener
public class DatabaseSetup implements ServletContextListener {
  @Override
  public void contextInitialized(ServletContextEvent e) {
    try {
      initialize();
    } catch (Exception ex) {
      throw new IllegalStateException(
          "Database initialization failed; check DB_URL, DB_USER and DB_PASSWORD.", ex);
    }
  }

  public static void initialize() throws Exception {
    try (Connection c = DBConnection.getConnection();
        InputStream in = DatabaseSetup.class.getResourceAsStream("/schema.sql")) {
      if (in == null) throw new IOException("Missing schema.sql");
      for (String sql : new String(in.readAllBytes(), StandardCharsets.UTF_8).split(";"))
        if (!sql.isBlank())
          try (Statement s = c.createStatement()) {
            s.execute(sql);
          }
      if (Settings.demo()) seed(c);
    }
  }

  private static void seed(Connection c) throws SQLException {
    try (Statement s = c.createStatement();
        ResultSet r = s.executeQuery("SELECT COUNT(*) FROM users")) {
      r.next();
      if (r.getInt(1) > 0) return;
    }
    c.setAutoCommit(false);
    try (Statement s = c.createStatement()) {
      s.executeUpdate(
          "INSERT INTO users(firebase_uid,email,full_name,role) VALUES"
              + " ('demo-admin','admin@smartpm.demo','Alex"
              + " Morgan','ADMIN'),('demo-manager','manager@smartpm.demo','Jordan"
              + " Lee','PROJECT_MANAGER'),('demo-member','member@smartpm.demo','Sam"
              + " Rivera','MEMBER'),('demo-designer','designer@smartpm.demo','Taylor"
              + " Chen','MEMBER')");
      s.executeUpdate(
          "INSERT INTO projects(name,description,start_date,deadline,status) VALUES ('Campus"
              + " Connect','A better way for students to discover clubs, events and"
              + " opportunities.',CURRENT_DATE,DATEADD('DAY',14,CURRENT_DATE),'IN_PROGRESS'),('Design"
              + " System','Reusable components and a consistent visual language for every"
              + " team.',CURRENT_DATE,DATEADD('DAY',28,CURRENT_DATE),'PLANNING'),('Review 2"
              + " Submission','Prepare the working project, test evidence and review"
              + " presentation.',CURRENT_DATE,DATEADD('DAY',6,CURRENT_DATE),'IN_PROGRESS')");
      s.executeUpdate(
          "INSERT INTO project_members(project_id,user_id,project_role) VALUES"
              + " (1,2,'OWNER'),(1,3,'MEMBER'),(1,4,'MEMBER'),(2,2,'OWNER'),(2,4,'MEMBER'),(3,2,'OWNER'),(3,3,'MEMBER')");
      s.executeUpdate(
          "INSERT INTO"
              + " tasks(project_id,title,description,assigned_to,created_by,priority,status,deadline)"
              + " VALUES (1,'Build event discovery page','Create searchable event cards with date"
              + " and category"
              + " filters.',3,2,'HIGH','IN_PROGRESS',DATEADD('DAY',2,CURRENT_DATE)),(1,'Map student"
              + " onboarding flow','Document the journey from sign-up to first"
              + " event.',4,2,'MEDIUM','REVIEW',DATEADD('DAY',3,CURRENT_DATE)),(1,'Set up database"
              + " schema','Define users, clubs and event"
              + " relationships.',3,2,'HIGH','COMPLETED',CURRENT_DATE),(1,'Test registration"
              + " validation','Cover empty fields, invalid dates and duplicate"
              + " registration.',3,2,'URGENT','TODO',DATEADD('DAY',-1,CURRENT_DATE)),(2,'Define"
              + " color and typography tokens','Document accessible text and surface"
              + " combinations.',4,2,'MEDIUM','IN_PROGRESS',DATEADD('DAY',5,CURRENT_DATE)),(2,'Create"
              + " button components','Include focus, loading and disabled"
              + " states.',4,2,'LOW','TODO',DATEADD('DAY',8,CURRENT_DATE)),(3,'Prepare demo"
              + " walkthrough','Show project creation, assignment and"
              + " completion.',3,2,'HIGH','TODO',DATEADD('DAY',4,CURRENT_DATE)),(3,'Document setup"
              + " instructions','Verify a clean installation using the"
              + " README.',2,2,'MEDIUM','COMPLETED',CURRENT_DATE)");
      s.executeUpdate(
          "INSERT INTO activity_logs(user_id,project_id,action) VALUES (2,1,'Created Campus Connect"
              + " and invited the team'),(3,1,'Completed Set up database schema'),(4,1,'Moved Map"
              + " student onboarding flow to review'),(2,3,'Created Review 2 Submission')");
      s.executeUpdate(
          "INSERT INTO task_comments(task_id,user_id,body) VALUES (1,2,'Please include keyboard"
              + " navigation in the event filters.')");
      c.commit();
    } catch (SQLException ex) {
      c.rollback();
      throw ex;
    } finally {
      c.setAutoCommit(true);
    }
  }
}

package com.smartprojectmanager;

import static org.junit.Assert.*;

import com.smartprojectmanager.dao.*;
import com.smartprojectmanager.model.User;
import com.smartprojectmanager.service.ProjectService;
import com.smartprojectmanager.util.DatabaseSetup;
import java.util.*;
import org.junit.*;

/** Runs against a fresh MySQL database when MYSQL_TEST_URL is supplied (also in CI). */
public class MysqlCompatibilityTest {
  @Test
  public void projectTaskAndReportQueriesWorkOnMysql() throws Exception {
    Assume.assumeTrue(
        "Set MYSQL_TEST_URL to run the real MySQL integration test.",
        System.getenv("MYSQL_TEST_URL") != null);
    System.setProperty("APP_DEMO", "false");
    System.setProperty("DB_URL", System.getenv("MYSQL_TEST_URL"));
    System.setProperty("DB_USER", System.getenv().getOrDefault("MYSQL_TEST_USER", "root"));
    System.setProperty("DB_PASSWORD", System.getenv().getOrDefault("MYSQL_TEST_PASSWORD", "test"));
    try {
      DatabaseSetup.initialize();
      UserDAO dao = new UserDAO();
      String suffix = UUID.randomUUID().toString();
      User u = new User();
      u.setFirebaseUid("ci-" + suffix);
      u.setEmail("ci-" + suffix + "@example.test");
      u.setFullName("CI Manager");
      u.setRole("PROJECT_MANAGER");
      assertTrue(dao.createUser(u));
      u = dao.getUserByFirebaseUid(u.getFirebaseUid());
      ProjectService service = new ProjectService();
      service.mutate(u, "createProject", Map.of("name", "MySQL integration " + suffix));
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> projects =
          (List<Map<String, Object>>) service.snapshot(u).get("projects");
      String id = projects.get(0).get("id").toString();
      try {
        service.mutate(
            u,
            "createTask",
            Map.of(
                "project_id",
                id,
                "title",
                "MySQL persisted task",
                "assigned_to",
                String.valueOf(u.getId()),
                "deadline",
                "2026-12-01"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks =
            (List<Map<String, Object>>) service.snapshot(u).get("tasks");
        assertEquals(1, tasks.size());
        String taskId = tasks.get(0).get("id").toString();
        service.mutate(
            u, "setStatus", Map.of("project_id", id, "task_id", taskId, "status", "COMPLETED"));
        service.mutate(
            u, "addComment", Map.of("project_id", id, "task_id", taskId, "body", "MySQL verified"));
        assertTrue(service.snapshot(u).get("comments").toString().contains("MySQL verified"));
      } finally {
        service.mutate(u, "deleteProject", Map.of("project_id", id));
      }
    } finally {
      for (String key : List.of("APP_DEMO", "DB_URL", "DB_USER", "DB_PASSWORD"))
        System.clearProperty(key);
    }
  }
}

package com.smartprojectmanager;

import static org.junit.Assert.*;

import com.smartprojectmanager.dao.*;
import com.smartprojectmanager.model.User;
import com.smartprojectmanager.service.ProjectService;
import com.smartprojectmanager.util.DatabaseSetup;
import java.sql.*;
import java.util.*;
import org.junit.*;

public class ProjectServiceTest {
  private ProjectService service;
  private User admin, manager, member;

  @Before
  public void setup() throws Exception {
    System.setProperty("APP_DEMO", "true");
    System.setProperty(
        "DB_URL",
        "jdbc:h2:mem:service"
            + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    DatabaseSetup.initialize();
    service = new ProjectService();
    UserDAO dao = new UserDAO();
    admin = dao.getUserByFirebaseUid("demo-admin");
    manager = dao.getUserByFirebaseUid("demo-manager");
    member = dao.getUserByFirebaseUid("demo-member");
  }

  @After
  public void cleanup() {
    System.clearProperty("APP_DEMO");
    System.clearProperty("DB_URL");
  }

  private Map<String, String> params(String... pairs) {
    Map<String, String> m = new HashMap<>();
    for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
    return m;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> list(User u, String key) throws Exception {
    return (List<Map<String, Object>>) service.snapshot(u).get(key);
  }

  private void rejected(int code, User u, String action, Map<String, String> p) throws Exception {
    try {
      service.mutate(u, action, p);
      fail("Expected access or validation rejection");
    } catch (ProjectService.Problem e) {
      assertEquals(code, e.status);
    }
  }

  @Test
  public void projectAndTaskLifecyclePersistsAndCascades() throws Exception {
    service.mutate(
        manager,
        "createProject",
        params(
            "name",
            "New product",
            "description",
            "A real project",
            "start_date",
            "2026-11-01",
            "deadline",
            "2026-12-01"));
    Map<String, Object> p = list(manager, "projects").get(0);
    String id = p.get("id").toString();
    service.mutate(manager, "addMember", params("project_id", id, "user_id", "3"));
    service.mutate(
        manager,
        "createTask",
        params(
            "project_id",
            id,
            "title",
            "Ship MVP",
            "assigned_to",
            "3",
            "priority",
            "HIGH",
            "deadline",
            "2026-11-30"));
    Map<String, Object> t =
        list(manager, "tasks").stream()
            .filter(x -> x.get("title").equals("Ship MVP"))
            .findFirst()
            .get();
    String tid = t.get("id").toString();
    service.mutate(
        member, "setStatus", params("project_id", id, "task_id", tid, "status", "COMPLETED"));
    service.mutate(
        member, "addComment", params("project_id", id, "task_id", tid, "body", "Ready for review"));
    assertTrue(
        list(manager, "tasks").stream()
            .anyMatch(
                x -> x.get("id").toString().equals(tid) && x.get("status").equals("COMPLETED")));
    assertTrue(
        list(manager, "comments").stream().anyMatch(x -> x.get("body").equals("Ready for review")));
    service.mutate(
        manager,
        "updateTask",
        params(
            "project_id",
            id,
            "task_id",
            tid,
            "title",
            "Ship v2",
            "status",
            "REVIEW",
            "priority",
            "URGENT",
            "assigned_to",
            "3"));
    assertTrue(list(manager, "tasks").stream().anyMatch(x -> x.get("title").equals("Ship v2")));
    service.mutate(manager, "deleteProject", params("project_id", id));
    assertFalse(list(admin, "tasks").stream().anyMatch(x -> x.get("id").toString().equals(tid)));
    assertFalse(
        list(admin, "comments").stream().anyMatch(x -> x.get("task_id").toString().equals(tid)));
  }

  @Test
  public void memberCannotCreateProjectsOrAssignTasks() throws Exception {
    rejected(403, member, "createProject", params("name", "Unauthorized"));
    rejected(403, member, "createTask", params("project_id", "1", "title", "Unauthorized"));
    rejected(403, member, "updateRole", params("user_id", "3", "role", "ADMIN"));
  }

  @Test
  public void inaccessibleProjectsAndTheirDataAreHidden() throws Exception {
    assertEquals(2, list(member, "projects").size());
    assertFalse(
        list(member, "tasks").stream()
            .anyMatch(t -> ((Number) t.get("project_id")).intValue() == 2));
    assertFalse(
        list(member, "comments").stream()
            .anyMatch(t -> ((Number) t.get("task_id")).intValue() == 5));
    rejected(
        403,
        member,
        "addComment",
        params("project_id", "2", "task_id", "5", "body", "Hidden project"));
    assertEquals(0, list(member, "users").size());
  }

  @Test
  public void memberCannotChangeAnotherPersonsTask() throws Exception {
    rejected(
        403, member, "setStatus", params("project_id", "1", "task_id", "2", "status", "COMPLETED"));
    service.mutate(
        member, "setStatus", params("project_id", "1", "task_id", "1", "status", "REVIEW"));
  }

  @Test
  public void assigneeMustBelongToProjectAndFailureRollsBack() throws Exception {
    int before = list(admin, "tasks").size(), activity = list(admin, "activity").size();
    rejected(
        400,
        manager,
        "createTask",
        params("project_id", "2", "title", "Invalid assignment", "assigned_to", "3"));
    assertEquals(before, list(admin, "tasks").size());
    assertEquals(activity, list(admin, "activity").size());
  }

  @Test
  public void removingMemberUnassignsTheirTasksAndRevokesAccess() throws Exception {
    service.mutate(manager, "removeMember", params("project_id", "1", "user_id", "3"));
    assertFalse(
        list(admin, "tasks").stream()
            .anyMatch(
                t ->
                    ((Number) t.get("project_id")).intValue() == 1
                        && Integer.valueOf(3).equals(t.get("assigned_to"))));
    rejected(
        403, member, "setStatus", params("project_id", "1", "task_id", "1", "status", "COMPLETED"));
    rejected(400, manager, "removeMember", params("project_id", "1", "user_id", "2"));
  }

  @Test
  public void invalidInputAndCrossProjectTaskIdsAreRejected() throws Exception {
    rejected(
        400, manager, "createTask", params("project_id", "1", "title", " ", "priority", "HIGH"));
    rejected(
        400,
        manager,
        "createTask",
        params("project_id", "1", "title", "Task", "deadline", "2026-02-30"));
    rejected(
        400,
        manager,
        "createTask",
        params("project_id", "1", "title", "Task", "priority", "SUPER"));
    rejected(
        404,
        manager,
        "setStatus",
        params("project_id", "1", "task_id", "5", "status", "COMPLETED"));
    rejected(
        400,
        manager,
        "createProject",
        params("name", "Invalid dates", "start_date", "2026-12-01", "deadline", "2026-11-01"));
  }

  @Test
  public void onlyAdminsCanChangeRolesAndCannotDemoteThemselves() throws Exception {
    rejected(403, manager, "updateRole", params("user_id", "3", "role", "ADMIN"));
    rejected(400, admin, "updateRole", params("user_id", "1", "role", "MEMBER"));
    service.mutate(admin, "updateRole", params("user_id", "3", "role", "PROJECT_MANAGER"));
    assertEquals("PROJECT_MANAGER", new UserDAO().getUserByFirebaseUid("demo-member").getRole());
  }

  @Test
  public void duplicateMembershipIsConflict() throws Exception {
    rejected(409, manager, "addMember", params("project_id", "1", "user_id", "3"));
  }

  @Test
  public void managerCannotManageAnUnrelatedProject() throws Exception {
    service.mutate(admin, "createProject", params("name", "Admin private"));
    String id = list(admin, "projects").get(0).get("id").toString();
    rejected(403, manager, "createTask", params("project_id", id, "title", "No access"));
  }

  @Test
  public void textPayloadIsStoredAsDataAndSqlCannotEscapeParameters() throws Exception {
    String payload = "<img src=x onerror=alert(1)>'); DROP TABLE users; --";
    service.mutate(manager, "createProject", params("name", payload));
    assertEquals(payload, list(manager, "projects").get(0).get("name"));
    assertEquals(4, list(admin, "users").size());
  }

  @Test
  public void demoCannotConnectToMysql() throws Exception {
    System.setProperty("DB_URL", "jdbc:mysql://localhost/production");
    try {
      DBConnection.getConnection();
      fail("Demo must not access MySQL");
    } catch (SQLException e) {
      assertTrue(e.getMessage().contains("isolated H2"));
    }
  }
}

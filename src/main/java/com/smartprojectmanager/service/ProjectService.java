package com.smartprojectmanager.service;

import com.smartprojectmanager.dao.DBConnection;
import com.smartprojectmanager.model.User;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** JDBC application boundary: every write is authorized, validated and transactional. */
public class ProjectService {
  public static class Problem extends RuntimeException {
    public final int status;

    public Problem(int status, String message) {
      super(message);
      this.status = status;
    }
  }

  private static void require(boolean condition, int status, String message) {
    if (!condition) throw new Problem(status, message);
  }

  private static List<Map<String, Object>> query(Connection c, String sql, Object... args)
      throws SQLException {
    try (PreparedStatement s = c.prepareStatement(sql)) {
      bind(s, args);
      try (ResultSet r = s.executeQuery()) {
        List<Map<String, Object>> rows = new ArrayList<>();
        while (r.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= r.getMetaData().getColumnCount(); i++) {
            Object v = r.getObject(i);
            row.put(
                r.getMetaData().getColumnLabel(i).toLowerCase(Locale.ROOT),
                v instanceof java.util.Date ? v.toString() : v);
          }
          rows.add(row);
        }
        return rows;
      }
    }
  }

  private static void bind(PreparedStatement s, Object... args) throws SQLException {
    for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
  }

  private static int write(Connection c, String sql, Object... args) throws SQLException {
    try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(s, args);
      s.executeUpdate();
      try (ResultSet r = s.getGeneratedKeys()) {
        return r.next() ? r.getInt(1) : 0;
      }
    }
  }

  private static String text(Map<String, String> p, String key, int max, boolean required) {
    String v = p.getOrDefault(key, "").trim();
    require(!required || !v.isEmpty(), 400, key + " is required.");
    require(v.length() <= max, 400, key + " is too long.");
    return v;
  }

  private static int number(Map<String, String> p, String key) {
    try {
      int n = Integer.parseInt(p.getOrDefault(key, "0"));
      require(n > 0, 400, "Invalid " + key);
      return n;
    } catch (NumberFormatException e) {
      throw new Problem(400, "Invalid " + key);
    }
  }

  private static String choice(
      Map<String, String> p, String key, String fallback, String... valid) {
    String value = p.getOrDefault(key, fallback);
    require(Arrays.asList(valid).contains(value), 400, "Invalid " + key);
    return value;
  }

  private static java.sql.Date date(Map<String, String> p, String key) {
    String value = text(p, key, 10, false);
    if (value.isEmpty()) return null;
    try {
      return java.sql.Date.valueOf(LocalDate.parse(value));
    } catch (Exception e) {
      throw new Problem(400, "Invalid " + key + "; use YYYY-MM-DD.");
    }
  }

  private static Map<String, Object> one(Connection c, String sql, Object... args)
      throws SQLException {
    List<Map<String, Object>> rows = query(c, sql, args);
    require(!rows.isEmpty(), 404, "Record not found.");
    return rows.get(0);
  }

  private static boolean admin(User u) {
    return "ADMIN".equals(u.getRole());
  }

  private static boolean canCreate(User u) {
    return admin(u) || "PROJECT_MANAGER".equals(u.getRole());
  }

  private static boolean manager(Connection c, User u, int project) throws SQLException {
    return admin(u)
        || !query(
                c,
                "SELECT user_id FROM project_members WHERE project_id=? AND user_id=? AND"
                    + " project_role IN ('OWNER','MANAGER')",
                project,
                u.getId())
            .isEmpty();
  }

  private static void access(Connection c, User u, int project) throws SQLException {
    one(c, "SELECT id FROM projects WHERE id=?", project);
    require(
        admin(u)
            || !query(
                    c,
                    "SELECT user_id FROM project_members WHERE project_id=? AND user_id=?",
                    project,
                    u.getId())
                .isEmpty(),
        403,
        "You do not have access to this project.");
  }

  private static void manage(Connection c, User u, int project) throws SQLException {
    access(c, u, project);
    require(manager(c, u, project), 403, "A project manager is required.");
  }

  private static void lock(Connection c, int project) throws SQLException {
    one(c, "SELECT id FROM projects WHERE id=? FOR UPDATE", project);
  }

  private static void log(Connection c, User u, int project, String action) throws SQLException {
    write(
        c,
        "INSERT INTO activity_logs(user_id,project_id,action) VALUES (?,?,?)",
        u.getId(),
        project,
        action);
  }

  public Map<String, Object> snapshot(User user) throws SQLException {
    try (Connection c = DBConnection.getConnection()) {
      Map<String, Object> out = new LinkedHashMap<>();
      Map<String, Object> me = new LinkedHashMap<>();
      me.put("id", user.getId());
      me.put("full_name", user.getFullName());
      me.put("email", user.getEmail());
      me.put("role", user.getRole());
      out.put("user", me);
      String scope =
          admin(user)
              ? ""
              : " WHERE p.id IN (SELECT project_id FROM project_members WHERE user_id="
                  + user.getId()
                  + ")";
      List<Map<String, Object>> projects =
          query(c, "SELECT p.* FROM projects p" + scope + " ORDER BY p.created_at DESC,p.id DESC");
      for (Map<String, Object> p : projects)
        p.put("can_manage", manager(c, user, ((Number) p.get("id")).intValue()));
      out.put("projects", projects);
      String ids =
          projects.isEmpty()
              ? "0"
              : projects.stream()
                  .map(p -> p.get("id").toString())
                  .reduce((a, b) -> a + "," + b)
                  .get();
      out.put(
          "tasks",
          query(
              c,
              "SELECT t.*,u.full_name AS assignee_name,p.name AS project_name FROM tasks t LEFT"
                  + " JOIN users u ON u.id=t.assigned_to JOIN projects p ON p.id=t.project_id WHERE"
                  + " t.project_id IN ("
                  + ids
                  + ") ORDER BY t.deadline IS NULL,t.deadline,t.id"));
      out.put(
          "members",
          query(
              c,
              "SELECT pm.*,u.full_name,u.email,u.role FROM project_members pm JOIN users u ON"
                  + " u.id=pm.user_id WHERE pm.project_id IN ("
                  + ids
                  + ")"));
      out.put(
          "activity",
          query(
              c,
              "SELECT a.*,u.full_name,p.name AS project_name FROM activity_logs a LEFT JOIN users u"
                  + " ON u.id=a.user_id JOIN projects p ON p.id=a.project_id WHERE a.project_id IN"
                  + " ("
                  + ids
                  + ") ORDER BY a.created_at DESC,a.id DESC LIMIT 30"));
      out.put(
          "comments",
          query(
              c,
              "SELECT tc.*,u.full_name FROM task_comments tc JOIN tasks t ON t.id=tc.task_id JOIN"
                  + " users u ON u.id=tc.user_id WHERE t.project_id IN ("
                  + ids
                  + ") ORDER BY tc.created_at,tc.id"));
      // Managers need the user directory to invite existing registered users.
      out.put(
          "users",
          canCreate(user)
              ? query(c, "SELECT id,full_name,email,role FROM users ORDER BY full_name")
              : Collections.emptyList());
      return out;
    }
  }

  public void mutate(User user, String action, Map<String, String> p) throws SQLException {
    try (Connection c = DBConnection.getConnection()) {
      c.setAutoCommit(false);
      try {
        apply(c, user, action, p);
        c.commit();
      } catch (SQLException | RuntimeException e) {
        c.rollback();
        throw e;
      }
    }
  }

  private void apply(Connection c, User u, String action, Map<String, String> p)
      throws SQLException {
    if ("createProject".equals(action)) {
      require(canCreate(u), 403, "Only administrators and project managers can create projects.");
      String name = text(p, "name", 255, true);
      java.sql.Date start = date(p, "start_date"), deadline = date(p, "deadline");
      require(
          start == null || deadline == null || !deadline.before(start),
          400,
          "Deadline cannot precede the start date.");
      int id =
          write(
              c,
              "INSERT INTO projects(name,description,start_date,deadline,status) VALUES"
                  + " (?,?,?,?,?)",
              name,
              text(p, "description", 5000, false),
              start,
              deadline,
              choice(p, "status", "PLANNING", "PLANNING", "IN_PROGRESS", "ON_HOLD", "COMPLETED"));
      write(
          c,
          "INSERT INTO project_members(project_id,user_id,project_role) VALUES (?,?,'OWNER')",
          id,
          u.getId());
      log(c, u, id, "Created project: " + name);
      return;
    }
    if ("updateRole".equals(action)) {
      require(admin(u), 403, "Administrator access required.");
      int id = number(p, "user_id");
      require(id != u.getId(), 400, "You cannot change your own administrator role.");
      one(c, "SELECT id FROM users WHERE id=?", id);
      write(
          c,
          "UPDATE users SET role=? WHERE id=?",
          choice(p, "role", "MEMBER", "ADMIN", "PROJECT_MANAGER", "MEMBER"),
          id);
      return;
    }
    int project = number(p, "project_id");
    lock(c, project);
    access(c, u, project);
    if ("updateProject".equals(action)) {
      manage(c, u, project);
      String name = text(p, "name", 255, true);
      java.sql.Date start = date(p, "start_date"), end = date(p, "deadline");
      require(
          start == null || end == null || !end.before(start),
          400,
          "Deadline cannot precede the start date.");
      write(
          c,
          "UPDATE projects SET name=?,description=?,start_date=?,deadline=?,status=? WHERE id=?",
          name,
          text(p, "description", 5000, false),
          start,
          end,
          choice(p, "status", "PLANNING", "PLANNING", "IN_PROGRESS", "ON_HOLD", "COMPLETED"),
          project);
      log(c, u, project, "Updated project: " + name);
      return;
    }
    if ("deleteProject".equals(action)) {
      manage(c, u, project);
      write(c, "DELETE FROM projects WHERE id=?", project);
      return;
    }
    if ("addMember".equals(action) || "removeMember".equals(action)) {
      manage(c, u, project);
      int member = number(p, "user_id");
      Map<String, Object> person = one(c, "SELECT full_name FROM users WHERE id=?", member);
      if ("addMember".equals(action)) {
        require(
            query(
                    c,
                    "SELECT user_id FROM project_members WHERE project_id=? AND user_id=?",
                    project,
                    member)
                .isEmpty(),
            409,
            "This person is already on the team.");
        write(
            c,
            "INSERT INTO project_members(project_id,user_id,project_role) VALUES (?,?,'MEMBER')",
            project,
            member);
        log(c, u, project, "Added team member: " + person.get("full_name"));
      } else {
        Map<String, Object> membership =
            one(
                c,
                "SELECT project_role FROM project_members WHERE project_id=? AND user_id=?",
                project,
                member);
        require(
            !"OWNER".equals(membership.get("project_role")),
            400,
            "The project owner cannot be removed.");
        write(
            c,
            "UPDATE tasks SET assigned_to=NULL,updated_at=CURRENT_TIMESTAMP WHERE project_id=? AND"
                + " assigned_to=?",
            project,
            member);
        write(c, "DELETE FROM project_members WHERE project_id=? AND user_id=?", project, member);
        log(c, u, project, "Removed team member: " + person.get("full_name"));
      }
      return;
    }
    if ("createTask".equals(action) || "updateTask".equals(action)) {
      manage(c, u, project);
      String title = text(p, "title", 255, true);
      Integer assignee = null;
      if (!p.getOrDefault("assigned_to", "").isBlank()) {
        assignee = number(p, "assigned_to");
        require(
            !query(
                    c,
                    "SELECT user_id FROM project_members WHERE project_id=? AND user_id=?",
                    project,
                    assignee)
                .isEmpty(),
            400,
            "Assignee must be a member of this project.");
      }
      String priority = choice(p, "priority", "MEDIUM", "LOW", "MEDIUM", "HIGH", "URGENT");
      String status = choice(p, "status", "TODO", "TODO", "IN_PROGRESS", "REVIEW", "COMPLETED");
      if ("createTask".equals(action)) {
        write(
            c,
            "INSERT INTO"
                + " tasks(project_id,title,description,assigned_to,created_by,priority,status,deadline)"
                + " VALUES (?,?,?,?,?,?,?,?)",
            project,
            title,
            text(p, "description", 5000, false),
            assignee,
            u.getId(),
            priority,
            status,
            date(p, "deadline"));
        log(c, u, project, "Created task: " + title);
      } else {
        int task = number(p, "task_id");
        one(c, "SELECT id FROM tasks WHERE id=? AND project_id=?", task, project);
        write(
            c,
            "UPDATE tasks SET"
                + " title=?,description=?,assigned_to=?,priority=?,status=?,deadline=?,updated_at=CURRENT_TIMESTAMP"
                + " WHERE id=?",
            title,
            text(p, "description", 5000, false),
            assignee,
            priority,
            status,
            date(p, "deadline"),
            task);
        log(c, u, project, "Updated task: " + title);
      }
      return;
    }
    if ("setStatus".equals(action) || "deleteTask".equals(action) || "addComment".equals(action)) {
      int task = number(p, "task_id");
      Map<String, Object> t =
          one(c, "SELECT * FROM tasks WHERE id=? AND project_id=?", task, project);
      if ("addComment".equals(action)) {
        write(
            c,
            "INSERT INTO task_comments(task_id,user_id,body) VALUES (?,?,?)",
            task,
            u.getId(),
            text(p, "body", 2000, true));
        log(c, u, project, "Commented on: " + t.get("title"));
        return;
      }
      if ("deleteTask".equals(action)) {
        manage(c, u, project);
        write(c, "DELETE FROM tasks WHERE id=?", task);
        log(c, u, project, "Deleted task: " + t.get("title"));
        return;
      }
      require(
          manager(c, u, project)
              || (t.get("assigned_to") != null
                  && ((Number) t.get("assigned_to")).intValue() == u.getId()),
          403,
          "Only the assignee or project manager can update this task.");
      String status = choice(p, "status", "TODO", "TODO", "IN_PROGRESS", "REVIEW", "COMPLETED");
      write(c, "UPDATE tasks SET status=?,updated_at=CURRENT_TIMESTAMP WHERE id=?", status, task);
      log(
          c,
          u,
          project,
          "Moved " + t.get("title") + " to " + status.toLowerCase(Locale.ROOT).replace('_', ' '));
      return;
    }
    throw new Problem(400, "Unknown action.");
  }
}

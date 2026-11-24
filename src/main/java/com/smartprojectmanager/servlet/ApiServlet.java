package com.smartprojectmanager.servlet;

import com.google.gson.Gson;
import com.smartprojectmanager.model.User;
import com.smartprojectmanager.service.ProjectService;
import com.smartprojectmanager.util.Csv;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/api/*")
public class ApiServlet extends HttpServlet {
  private final ProjectService service = new ProjectService();
  private final Gson json = new Gson();

  private User user(HttpServletRequest r) {
    return (User) r.getSession(false).getAttribute("user");
  }

  @Override
  protected void doGet(HttpServletRequest r, HttpServletResponse s) throws IOException {
    if ("/report.csv".equals(r.getPathInfo())) {
      try {
        Map<String, Object> workspace = service.snapshot(user(r));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> tasks = (List<Map<String, Object>>) workspace.get("tasks");
        s.setContentType("text/csv;charset=UTF-8");
        s.setHeader("Cache-Control", "no-store");
        s.setHeader("Content-Disposition", "attachment; filename=\"smartpm-task-report.csv\"");
        s.getWriter()
            .print(
                "\uFEFF"
                    + Csv.row("Project", "Task", "Assignee", "Priority", "Status", "Deadline"));
        for (Map<String, Object> task : tasks)
          s.getWriter()
              .print(
                  Csv.row(
                      task.get("project_name"),
                      task.get("title"),
                      task.get("assignee_name"),
                      task.get("priority"),
                      task.get("status"),
                      task.get("deadline")));
      } catch (SQLException e) {
        getServletContext().log("Report export failed", e);
        s.sendError(500, "Report unavailable.");
      }
      return;
    }
    s.setContentType("application/json;charset=UTF-8");
    s.setHeader("Cache-Control", "no-store");
    if (!"/workspace".equals(r.getPathInfo())) {
      s.setStatus(404);
      s.getWriter().print("{\"error\":\"Endpoint not found\"}");
      return;
    }
    try {
      s.getWriter().print(json.toJson(service.snapshot(user(r))));
    } catch (SQLException e) {
      getServletContext().log("Workspace read failed", e);
      s.setStatus(500);
      s.getWriter().print("{\"error\":\"Database unavailable. Please try again.\"}");
    }
  }

  @Override
  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    r.setCharacterEncoding("UTF-8");
    s.setContentType("application/json;charset=UTF-8");
    s.setHeader("Cache-Control", "no-store");
    Map<String, String> params = new HashMap<>();
    r.getParameterMap().forEach((k, v) -> params.put(k, v[0]));
    try {
      service.mutate(user(r), r.getPathInfo() == null ? "" : r.getPathInfo().substring(1), params);
      s.getWriter().print("{\"success\":true}");
    } catch (ProjectService.Problem e) {
      s.setStatus(e.status);
      s.getWriter().print(json.toJson(Collections.singletonMap("error", e.getMessage())));
    } catch (SQLException e) {
      getServletContext().log("Workspace write failed", e);
      s.setStatus(500);
      s.getWriter().print("{\"error\":\"Changes could not be saved. Please try again.\"}");
    }
  }
}

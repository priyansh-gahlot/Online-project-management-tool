package com.smartprojectmanager.servlet;

import com.smartprojectmanager.dao.UserDAO;
import com.smartprojectmanager.model.User;
import com.smartprojectmanager.util.Settings;
import java.io.IOException;
import java.util.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/auth/demo")
public class DemoServlet extends HttpServlet {
  @Override
  protected void doPost(HttpServletRequest r, HttpServletResponse s) throws IOException {
    if (!Settings.demo()) {
      s.sendError(404);
      return;
    }
    String origin = r.getHeader("Origin");
    String expected =
        r.getScheme()
            + "://"
            + r.getServerName()
            + (r.getServerPort() == 80 || r.getServerPort() == 443 ? "" : ":" + r.getServerPort());
    if (origin != null && !origin.equals(expected)) {
      s.sendError(403);
      return;
    }
    String role = r.getParameter("role");
    if (!Arrays.asList("admin", "manager", "member").contains(role)) {
      s.sendError(400);
      return;
    }
    User u = new UserDAO().getUserByFirebaseUid("demo-" + role);
    if (u == null) {
      s.sendError(503);
      return;
    }
    if (r.getSession(false) != null) r.getSession(false).invalidate();
    HttpSession session = r.getSession(true);
    session.setAttribute("user", u);
    session.setAttribute("csrf", UUID.randomUUID().toString());
    s.sendRedirect(r.getContextPath() + "/dashboard");
  }
}

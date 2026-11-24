package com.smartprojectmanager.servlet;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet({"/dashboard", "/projects", "/tasks", "/team", "/reports", "/admin/users", "/profile"})
public class DashboardServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    r.getRequestDispatcher("/WEB-INF/dashboard.jsp").forward(r, s);
  }
}

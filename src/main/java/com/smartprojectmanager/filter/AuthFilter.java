package com.smartprojectmanager.filter;

import com.smartprojectmanager.dao.UserDAO;
import com.smartprojectmanager.model.User;
import java.io.IOException;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;

@WebFilter("/*")
public class AuthFilter implements Filter {
  @Override
  public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest r = (HttpServletRequest) req;
    HttpServletResponse s = (HttpServletResponse) res;
    s.setHeader("X-Content-Type-Options", "nosniff");
    s.setHeader("X-Frame-Options", "DENY");
    s.setHeader("Referrer-Policy", "same-origin");
    String path = r.getRequestURI().substring(r.getContextPath().length());
    if (path.startsWith("/assets/")
        || path.equals("/login.jsp")
        || path.equals("/register.jsp")
        || path.equals("/auth/verify")
        || path.equals("/auth/demo")) {
      chain.doFilter(req, res);
      return;
    }
    HttpSession session = r.getSession(false);
    User u = session == null ? null : (User) session.getAttribute("user");
    if (u != null) {
      u = new UserDAO().getUserByFirebaseUid(u.getFirebaseUid());
      if (u != null) session.setAttribute("user", u);
    }
    if (u == null) {
      if (path.startsWith("/api/")) {
        s.setStatus(401);
        s.setContentType("application/json");
        s.getWriter().print("{\"error\":\"Please sign in again.\"}");
      } else s.sendRedirect(r.getContextPath() + "/login.jsp");
      return;
    }
    if (session.getAttribute("csrf") == null)
      session.setAttribute("csrf", UUID.randomUUID().toString());
    if ("POST".equals(r.getMethod())
        && !session.getAttribute("csrf").equals(r.getHeader("X-CSRF-Token"))) {
      s.setStatus(403);
      s.setContentType("application/json");
      s.getWriter().print("{\"error\":\"Session check failed. Reload the page and try again.\"}");
      return;
    }
    s.setHeader("Cache-Control", "no-store");
    chain.doFilter(req, res);
  }
}

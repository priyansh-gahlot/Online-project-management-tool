package com.smartprojectmanager.servlet;

import com.smartprojectmanager.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }

        User user = (User) session.getAttribute("user");
        
        // In a real implementation, we would load statistics from DAOs based on the role here.
        // For example:
        if ("ADMIN".equals(user.getRole())) {
            request.setAttribute("totalUsers", 15);
            request.setAttribute("totalProjects", 8);
            request.setAttribute("activeTasks", 42);
        } else if ("PROJECT_MANAGER".equals(user.getRole())) {
            request.setAttribute("myProjects", 3);
            request.setAttribute("teamMembers", 12);
        } else {
            request.setAttribute("assignedTasks", 5);
            request.setAttribute("completedTasks", 18);
        }

        request.getRequestDispatcher("/WEB-INF/dashboard.jsp").forward(request, response);
    }
}

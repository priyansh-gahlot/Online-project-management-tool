package com.smartprojectmanager.servlet;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.smartprojectmanager.dao.UserDAO;
import com.smartprojectmanager.model.User;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/auth/verify")
public class AuthServlet extends HttpServlet {

    private UserDAO userDAO;

    @Override
    public void init() throws ServletException {
        userDAO = new UserDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String idToken = request.getParameter("idToken");
        String fullName = request.getParameter("fullName"); // optional, provided on registration

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        if (idToken == null || idToken.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"error\": \"Missing ID token\"}");
            return;
        }

        try {
            // Verify token with Firebase Admin SDK
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            // Resolve application user
            User user = userDAO.getUserByFirebaseUid(uid);

            if (user == null) {
                // New user registration flow
                user = new User();
                user.setFirebaseUid(uid);
                user.setEmail(email);
                user.setFullName(fullName != null && !fullName.trim().isEmpty() ? fullName : "Unknown User");
                user.setRole("MEMBER"); // Default role
                
                boolean created = userDAO.createUser(user);
                if (!created) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    out.print("{\"error\": \"Failed to create user in database\"}");
                    return;
                }
                user = userDAO.getUserByFirebaseUid(uid); // Re-fetch to get ID and timestamp
            }

            // Create session
            HttpSession session = request.getSession(true);
            session.setAttribute("user", user);

            out.print("{\"success\": true, \"redirect\": \"/dashboard\"}");

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\": \"Invalid token or authentication error\"}");
        }
    }
}

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Register - Smart Project Manager</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body class="auth-body">
    <div class="auth-container">
        <div class="auth-card">
            <h2>Create Account</h2>
            <p class="auth-subtitle">Join Smart Project Manager</p>
            
            <div id="error-message" class="alert alert-error" style="display: none;"></div>
            
            <form id="register-form">
                <div class="form-group">
                    <label for="fullName">Full Name</label>
                    <input type="text" id="fullName" required placeholder="John Doe">
                </div>
                <div class="form-group">
                    <label for="email">Email</label>
                    <input type="email" id="email" required placeholder="name@example.com">
                </div>
                <div class="form-group">
                    <label for="password">Password</label>
                    <input type="password" id="password" required placeholder="••••••••" minlength="6">
                </div>
                <button type="submit" class="btn btn-primary btn-block" id="register-btn">Sign Up</button>
            </form>
            
            <p class="auth-footer">
                Already have an account? <a href="${pageContext.request.contextPath}/login.jsp">Sign in</a>
            </p>
        </div>
    </div>

    <!-- Firebase SDKs -->
    <script src="https://www.gstatic.com/firebasejs/9.22.1/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/9.22.1/firebase-auth-compat.js"></script>
    <!-- App Scripts -->
    <script src="${pageContext.request.contextPath}/assets/js/firebase-config.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/auth.js"></script>
</body>
</html>

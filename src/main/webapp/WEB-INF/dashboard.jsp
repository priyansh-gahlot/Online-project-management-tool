<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - Smart Project Manager</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/dashboard.css">
</head>
<body>
    <div class="layout">
        <!-- Sidebar -->
        <aside class="sidebar">
            <div class="sidebar-header">
                <h2>SmartPM</h2>
            </div>
            <nav class="sidebar-nav">
                <a href="${pageContext.request.contextPath}/dashboard" class="nav-item active">Dashboard</a>
                <c:choose>
                    <c:when test="${sessionScope.user.role == 'ADMIN'}">
                        <a href="${pageContext.request.contextPath}/admin/users" class="nav-item">Manage Users</a>
                        <a href="${pageContext.request.contextPath}/admin/projects" class="nav-item">All Projects</a>
                        <a href="${pageContext.request.contextPath}/admin/settings" class="nav-item">Settings</a>
                    </c:when>
                    <c:when test="${sessionScope.user.role == 'PROJECT_MANAGER'}">
                        <a href="${pageContext.request.contextPath}/projects" class="nav-item">My Projects</a>
                        <a href="${pageContext.request.contextPath}/team" class="nav-item">Team Members</a>
                        <a href="${pageContext.request.contextPath}/reports" class="nav-item">Reports</a>
                    </c:when>
                    <c:otherwise>
                        <a href="${pageContext.request.contextPath}/projects" class="nav-item">Projects</a>
                        <a href="${pageContext.request.contextPath}/tasks" class="nav-item">My Tasks</a>
                    </c:otherwise>
                </c:choose>
            </nav>
            <div class="sidebar-footer">
                <a href="${pageContext.request.contextPath}/profile" class="nav-item">Profile</a>
                <button id="logout-btn" class="nav-item btn-logout">Logout</button>
            </div>
        </aside>

        <!-- Main Content -->
        <main class="main-content">
            <header class="topbar">
                <div class="page-title">
                    <h1>Dashboard</h1>
                </div>
                <div class="user-menu">
                    Welcome, <strong>${sessionScope.user.fullName}</strong> (${sessionScope.user.role})
                </div>
            </header>

            <div class="content-wrapper">
                
                <c:choose>
                    <%-- ADMIN DASHBOARD --%>
                    <c:when test="${sessionScope.user.role == 'ADMIN'}">
                        <div class="stats-grid">
                            <div class="stat-card">
                                <h3>Total Users</h3>
                                <div class="stat-value">${totalUsers}</div>
                            </div>
                            <div class="stat-card">
                                <h3>Total Projects</h3>
                                <div class="stat-value">${totalProjects}</div>
                            </div>
                            <div class="stat-card">
                                <h3>Active Tasks</h3>
                                <div class="stat-value">${activeTasks}</div>
                            </div>
                        </div>
                        <div class="dashboard-panel">
                            <h2>System Activity Overview</h2>
                            <p class="placeholder-text">Charts and system logs will appear here.</p>
                        </div>
                    </c:when>

                    <%-- PROJECT MANAGER DASHBOARD --%>
                    <c:when test="${sessionScope.user.role == 'PROJECT_MANAGER'}">
                        <div class="stats-grid">
                            <div class="stat-card">
                                <h3>My Projects</h3>
                                <div class="stat-value">${myProjects}</div>
                            </div>
                            <div class="stat-card">
                                <h3>Team Members</h3>
                                <div class="stat-value">${teamMembers}</div>
                            </div>
                        </div>
                        <div class="dashboard-panel">
                            <h2>Project Progress</h2>
                            <p class="placeholder-text">Project timeline and task completion charts will appear here.</p>
                            <button class="btn btn-primary">Create New Project</button>
                        </div>
                    </c:when>

                    <%-- TEAM MEMBER DASHBOARD --%>
                    <c:otherwise>
                        <div class="stats-grid">
                            <div class="stat-card">
                                <h3>Assigned Tasks</h3>
                                <div class="stat-value">${assignedTasks}</div>
                            </div>
                            <div class="stat-card">
                                <h3>Completed Tasks</h3>
                                <div class="stat-value">${completedTasks}</div>
                            </div>
                        </div>
                        <div class="dashboard-panel">
                            <h2>My Upcoming Deadlines</h2>
                            <p class="placeholder-text">List of pending tasks sorted by deadline will appear here.</p>
                        </div>
                    </c:otherwise>
                </c:choose>

            </div>
        </main>
    </div>

    <!-- Firebase SDKs for logout -->
    <script src="https://www.gstatic.com/firebasejs/9.22.1/firebase-app-compat.js"></script>
    <script src="https://www.gstatic.com/firebasejs/9.22.1/firebase-auth-compat.js"></script>
    <script src="${pageContext.request.contextPath}/assets/js/firebase-config.js"></script>
    
    <script>
        document.getElementById('logout-btn').addEventListener('click', async () => {
            try {
                // Sign out of Firebase
                await auth.signOut();
                
                // Clear Java Session
                const response = await fetch('${pageContext.request.contextPath}/auth/logout', { method: 'POST' });
                if (response.ok) {
                    window.location.href = '${pageContext.request.contextPath}/login.jsp';
                }
            } catch (error) {
                console.error('Logout error:', error);
            }
        });
    </script>
</body>
</html>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
 <meta name="app-context" content="${pageContext.request.contextPath}">
 <meta name="csrf-token" content="${sessionScope.csrf}">
 <title>SmartPM · Workspace</title>
 <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/workspace.css">
 <script defer src="${pageContext.request.contextPath}/assets/js/workspace.js"></script>
</head>
<body>
 <a class="skip-link" href="#main">Skip to content</a>
 <aside class="sidebar">
  <a href="#dashboard" class="brand"><span class="brand-mark">S</span>SmartPM<span class="brand-dot">.</span></a>
  <div class="workspace-label"><span class="workspace-icon">T</span><div>The workspace<small>Make great things together</small></div></div>
  <p class="nav-caption">WORKSPACE</p>
  <nav id="navigation" aria-label="Workspace"></nav>
  <div class="sidebar-note"><span class="spark">✦</span><strong>A little more focus.<br>A lot more progress.</strong><p>Your team's next big idea starts with one small task.</p></div>
  <button id="logout" class="logout">↗ &nbsp; Sign out</button>
 </aside>
 <div class="app">
  <header class="topbar"><div class="breadcrumb">Workspace <span>/</span> <strong id="breadcrumb">Overview</strong></div><div class="topbar-right"><span class="today" id="today"></span><button id="profile" class="avatar" aria-label="Your profile"></button></div></header>
  <main id="main" tabindex="-1"><div class="loading">Loading your workspace…</div></main>
  <footer>Smart Project Manager <span>Built for teams that move ideas forward.</span></footer>
 </div>
 <dialog id="modal" aria-labelledby="modal-title"><div id="modal-content"></div></dialog>
 <div id="toast" role="status" aria-live="polite"></div>
</body>
</html>

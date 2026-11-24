package com.smartprojectmanager;

import static org.junit.Assert.*;

import java.io.File;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.*;
import org.apache.catalina.*;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.*;
import org.junit.*;

public class HttpWorkflowTest {
  private static Tomcat tomcat;
  private static String base;
  private HttpClient client;

  @BeforeClass
  public static void start() throws Exception {
    System.setProperty("APP_DEMO", "true");
    System.setProperty(
        "DB_URL", "jdbc:h2:mem:http;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    tomcat = new Tomcat();
    tomcat.setBaseDir("target/test-tomcat");
    tomcat.setPort(0);
    tomcat.getConnector().setProperty("address", "127.0.0.1");
    Context context = tomcat.addWebapp("/smartpm", new File("src/main/webapp").getAbsolutePath());
    context.setParentClassLoader(HttpWorkflowTest.class.getClassLoader());
    StandardRoot root = new StandardRoot(context);
    root.addPreResources(
        new DirResourceSet(
            root, "/WEB-INF/classes", new File("target/classes").getAbsolutePath(), "/"));
    context.setResources(root);
    tomcat.start();
    base = "http://127.0.0.1:" + tomcat.getConnector().getLocalPort() + "/smartpm";
  }

  @AfterClass
  public static void stop() throws Exception {
    try {
      tomcat.stop();
      tomcat.destroy();
    } finally {
      System.clearProperty("APP_DEMO");
      System.clearProperty("DB_URL");
    }
  }

  @Before
  public void client() {
    client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .build();
  }

  private HttpResponse<String> get(String path) throws Exception {
    return client.send(
        HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> post(String path, String body, String token) throws Exception {
    HttpRequest.Builder b =
        HttpRequest.newBuilder(URI.create(base + path))
            .header("Content-Type", "application/x-www-form-urlencoded");
    if (token != null) b.header("X-CSRF-Token", token);
    return client.send(
        b.POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private String login(String role) throws Exception {
    assertEquals(302, post("/auth/demo", "role=" + role, null).statusCode());
    HttpResponse<String> page = get("/dashboard");
    assertEquals(page.body(), 200, page.statusCode());
    Matcher m = Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"").matcher(page.body());
    assertTrue(m.find());
    return m.group(1);
  }

  @Test
  public void authenticationAndContextPathWork() throws Exception {
    assertEquals(401, get("/api/workspace").statusCode());
    assertEquals(302, get("/dashboard").statusCode());
    HttpResponse<String> login = get("/login.jsp");
    assertEquals(200, login.statusCode());
    assertTrue(login.body().contains("action=\"/smartpm/auth/demo\""));
    assertTrue(login.body().contains("name=\"app-context\" content=\"/smartpm\""));
    String token = login("manager");
    assertTrue(token.length() > 20);
    HttpResponse<String> data = get("/api/workspace");
    assertEquals(200, data.statusCode());
    assertTrue(data.body().contains("Campus Connect"));
    assertEquals(404, get("/api/no-such-endpoint").statusCode());
  }

  @Test
  public void writesRequireCsrfAndPersistInWorkspace() throws Exception {
    String token = login("manager");
    assertEquals(403, post("/api/createProject", "name=Missing+token", null).statusCode());
    assertEquals(200, post("/api/createProject", "name=HTTP+workflow+project", token).statusCode());
    assertTrue(get("/api/workspace").body().contains("HTTP workflow project"));
    assertEquals(400, post("/api/createProject", "name=", token).statusCode());
    assertEquals(200, post("/auth/logout", "", token).statusCode());
    assertEquals(401, get("/api/workspace").statusCode());
  }

  @Test
  public void memberCannotElevatePrivilegesOverHttp() throws Exception {
    String token = login("member");
    assertEquals(403, post("/api/createProject", "name=Forbidden", token).statusCode());
    assertEquals(403, post("/api/updateRole", "user_id=3&role=ADMIN", token).statusCode());
  }

  @Test
  public void demoRejectsCrossOriginLogin() throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create(base + "/auth/demo"))
            .header("Origin", "https://other.example")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("role=admin"))
            .build();
    assertEquals(403, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
  }

  @Test
  public void reportDownloadReturnsCsvFromTheScopedDatabase() throws Exception {
    login("member");
    HttpResponse<String> report = get("/api/report.csv");
    assertEquals(200, report.statusCode());
    assertTrue(
        report
            .headers()
            .firstValue("Content-Disposition")
            .orElse("")
            .contains("smartpm-task-report.csv"));
    assertTrue(report.body().contains("Build event discovery page"));
    assertFalse(report.body().contains("Define color and typography tokens"));
  }

  @Test
  public void demoEndpointIsUnavailableWhenDemoIsDisabled() throws Exception {
    System.setProperty("APP_DEMO", "false");
    try {
      assertEquals(404, post("/auth/demo", "role=admin", null).statusCode());
    } finally {
      System.setProperty("APP_DEMO", "true");
    }
  }
}

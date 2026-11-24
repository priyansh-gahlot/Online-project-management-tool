package com.smartprojectmanager;

import com.smartprojectmanager.util.Settings;
import java.io.File;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.*;

/** Loopback-only development runner. Deploy the WAR on Tomcat 9 for hosting. */
public final class DevServer {
  public static void main(String[] args) throws Exception {
    Tomcat t = new Tomcat();
    t.setBaseDir("target/tomcat");
    t.setPort(Integer.parseInt(Settings.get("PORT", "8080")));
    t.getConnector().setProperty("address", "127.0.0.1");
    Context c = t.addWebapp("", new File("src/main/webapp").getAbsolutePath());
    c.setParentClassLoader(DevServer.class.getClassLoader());
    StandardRoot r = new StandardRoot(c);
    r.addPreResources(
        new DirResourceSet(
            r, "/WEB-INF/classes", new File("target/classes").getAbsolutePath(), "/"));
    c.setResources(r);
    t.start();
    System.out.println("SmartPM: http://localhost:" + Settings.get("PORT", "8080"));
    t.getServer().await();
  }
}

package com.smartprojectmanager;

import com.smartprojectmanager.dao.DBConnection;
import org.junit.Test;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBConnectionTest {

    @Test
    public void testDatabaseConnectionAndQuery() {
        // Ensure credentials are provided via environment variables, otherwise it defaults to "root"/""
        String dbUser = System.getenv("DB_USER");
        if (dbUser == null) {
            System.out.println("No DB_USER environment variable found. Defaulting to 'root'.");
        }

        System.out.println("Attempting to connect to the database...");

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull("Connection should not be null", conn);
            System.out.println("✅ Successfully connected to the database!");

            // Test a basic query on the existing schema
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM users")) {

                assertTrue("Should return a result row", rs.next());
                int count = rs.getInt("total");
                
                System.out.println("✅ Successfully queried the 'users' table.");
                System.out.println("Current number of users: " + count);
                
                assertTrue("Count should be >= 0", count >= 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Database connection or query failed: " + e.getMessage());
        }
    }
}

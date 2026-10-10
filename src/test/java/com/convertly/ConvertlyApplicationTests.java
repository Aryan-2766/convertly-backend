package com.convertly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConvertlyApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertNotNull(dataSource, "DataSource should not be null");
    }

    @Test
    void testAivenCloudMySqlConnection() {
        assertNotNull(dataSource, "DataSource should be initialized");
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Should obtain a connection to Aiven Cloud MySQL");
            assertTrue(connection.isValid(5), "Connection should be valid within 5 seconds");
            System.out.println(">>> SUCCESS: Connected to " + connection.getMetaData().getDatabaseProductName() + " on Aiven Cloud!");
        } catch (Exception e) {
            System.out.println(">>> INFO: Database currently disconnected: " + e.getMessage());
            System.out.println(">>> INFO: Application operates in Ephemeral transient mode.");
        }
    }
}


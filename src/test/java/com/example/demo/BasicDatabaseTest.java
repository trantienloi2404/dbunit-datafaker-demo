package com.example.demo;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test class demonstrating DBUnit operations with static XML datasets.
 * Shows fundamental DBUnit functionality for database testing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicDatabaseTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicDatabaseTest.class);

    private IDatabaseConnection connection;
    private IDataSet testDataSet;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up database connection and test data");

        // Create database connection
        connection = DatabaseTestUtils.createDatabaseConnection();

        // Load test dataset
        testDataSet = DatabaseTestUtils.loadDataSet("datasets/complete-test-data.xml");

        // Setup test data (clean insert)
        DatabaseTestUtils.setupTestData(connection, testDataSet);

        // Commit the transaction
        DatabaseTestUtils.commit(connection);

        logger.info("Test setup completed");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                logger.info("Cleaning up test data");
                DatabaseTestUtils.cleanupTestData(connection, testDataSet);
                logger.info("Test cleanup completed");
            } finally {
                DatabaseTestUtils.closeConnection(connection);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test Database Connection")
    void testDatabaseConnection() throws Exception {
        assertNotNull(connection, "Database connection should not be null");
        assertNotNull(connection.getConnection(), "JDBC connection should not be null");
        assertFalse(connection.getConnection().isClosed(), "Connection should be open");

        logger.info("Database connection test passed");
    }

    @Test
    @Order(2)
    @DisplayName("Test Dataset Loading")
    void testDatasetLoading() throws Exception {
        assertNotNull(testDataSet, "Test dataset should not be null");

        // Check if all expected tables are present
        String[] expectedTables = { "users", "products", "orders", "order_items", "reviews" };

        for (String tableName : expectedTables) {
            ITable table = testDataSet.getTable(tableName);
            assertNotNull(table, "Table " + tableName + " should exist in dataset");
            assertTrue(table.getRowCount() > 0, "Table " + tableName + " should have data");

            logger.info("Table {}: {} rows", tableName, table.getRowCount());
        }

        logger.info("Dataset loading test passed");
    }

    @Test
    @Order(3)
    @DisplayName("Test Users Table Data")
    void testUsersTableData() throws Exception {
        ITable usersTable = testDataSet.getTable("users");

        // Check total number of users
        assertEquals(3, usersTable.getRowCount(), "Should have 3 users in test data");

        // Check specific user data
        assertEquals("john_demo", usersTable.getValue(0, "username"));
        assertEquals("john@demo.com", usersTable.getValue(0, "email"));
        assertEquals("John", usersTable.getValue(0, "first_name"));
        assertEquals("Demo", usersTable.getValue(0, "last_name"));

        // Verify data in actual database
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM users WHERE is_active = true")) {

            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1), "Database should contain 3 active users");
        }

        logger.info("Users table data test passed");
    }
}
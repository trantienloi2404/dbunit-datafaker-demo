package com.example.demo;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
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
                // Clean up test data
                DatabaseTestUtils.cleanupTestData(connection, testDataSet);
                DatabaseTestUtils.commit(connection);

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

    @Test
    @Order(4)
    @DisplayName("Test Products Table Data")
    void testProductsTableData() throws Exception {
        ITable productsTable = testDataSet.getTable("products");

        // Check total number of products
        assertEquals(3, productsTable.getRowCount(), "Should have 3 products in test data");

        // Check specific product data
        assertEquals("Test Laptop", productsTable.getValue(0, "name"));
        assertEquals("Electronics", productsTable.getValue(0, "category"));
        assertEquals("LAP-TEST-001", productsTable.getValue(0, "sku"));

        // Verify data in actual database
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM products WHERE is_available = true")) {

            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1), "Database should contain 3 available products");
        }

        logger.info("Products table data test passed");
    }

    @Test
    @Order(5)
    @DisplayName("Test Orders and Order Items Relationship")
    void testOrdersAndOrderItemsRelationship() throws Exception {
        ITable ordersTable = testDataSet.getTable("orders");
        ITable orderItemsTable = testDataSet.getTable("order_items");

        // Check orders count
        assertEquals(2, ordersTable.getRowCount(), "Should have 2 orders in test data");

        // Check order items count
        assertEquals(3, orderItemsTable.getRowCount(), "Should have 3 order items in test data");

        // Verify relationships in database
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "SELECT o.order_number, COUNT(oi.id) as item_count " +
                        "FROM orders o " +
                        "LEFT JOIN order_items oi ON o.id = oi.order_id " +
                        "GROUP BY o.id, o.order_number " +
                        "ORDER BY o.id")) {

            ResultSet rs = stmt.executeQuery();

            // First order should have 2 items
            assertTrue(rs.next());
            assertEquals("ORD-TEST-001", rs.getString("order_number"));
            assertEquals(2, rs.getInt("item_count"));

            // Second order should have 1 item
            assertTrue(rs.next());
            assertEquals("ORD-TEST-002", rs.getString("order_number"));
            assertEquals(1, rs.getInt("item_count"));
        }

        logger.info("Orders and order items relationship test passed");
    }

    @Test
    @Order(6)
    @DisplayName("Test Reviews Table Data")
    void testReviewsTableData() throws Exception {
        ITable reviewsTable = testDataSet.getTable("reviews");

        // Check total number of reviews
        assertEquals(3, reviewsTable.getRowCount(), "Should have 3 reviews in test data");

        // Check review ratings (convert string values from XML to integers)
        assertEquals(5, Integer.parseInt((String) reviewsTable.getValue(0, "rating")));
        assertEquals(4, Integer.parseInt((String) reviewsTable.getValue(1, "rating")));
        assertEquals(3, Integer.parseInt((String) reviewsTable.getValue(2, "rating")));

        // Verify average rating calculation
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "SELECT AVG(rating) as avg_rating FROM reviews")) {

            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(4.0, rs.getDouble("avg_rating"), 0.1,
                    "Average rating should be 4.0");
        }

        logger.info("Reviews table data test passed");
    }

    @Test
    @Order(7)
    @DisplayName("Test Database Constraints")
    void testDatabaseConstraints() throws Exception {
        // Test foreign key constraints
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "SELECT u.username, COUNT(o.id) as order_count " +
                        "FROM users u " +
                        "LEFT JOIN orders o ON u.id = o.user_id " +
                        "GROUP BY u.id, u.username " +
                        "ORDER BY u.id")) {

            ResultSet rs = stmt.executeQuery();

            // john_demo should have 1 order
            assertTrue(rs.next());
            assertEquals("john_demo", rs.getString("username"));
            assertEquals(1, rs.getInt("order_count"));

            // jane_demo should have 1 order
            assertTrue(rs.next());
            assertEquals("jane_demo", rs.getString("username"));
            assertEquals(1, rs.getInt("order_count"));

            // bob_demo should have 0 orders
            assertTrue(rs.next());
            assertEquals("bob_demo", rs.getString("username"));
            assertEquals(0, rs.getInt("order_count"));
        }

        logger.info("Database constraints test passed");
    }

    @Test
    @Order(100)
    @DisplayName("Insert Performance with Large Dataset")
    void testInsertPerformanceLargeDataset() throws Exception {
        System.out.println("Loading large dataset for insert performance test");
        IDataSet largeDataSet = DatabaseTestUtils.loadDataSet("datasets/large-dataset.xml");
        long start = System.currentTimeMillis();
        DatabaseTestUtils.setupTestData(connection, largeDataSet);
        DatabaseTestUtils.commit(connection);
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Insert performance: " + elapsed + " ms");
        // Assert insert within 30 seconds
        assertTrue(elapsed < 30_000, "Insert should complete within 30 seconds, actual: " + elapsed + " ms");
    }

    @Test
    @Order(101)
    @DisplayName("Query Performance with Large Dataset")
    void testQueryPerformanceLargeDataset() throws Exception {
        System.out.println("Loading large dataset for query performance test");
        IDataSet largeDataSet = DatabaseTestUtils.loadDataSet("datasets/large-dataset.xml");
        DatabaseTestUtils.setupTestData(connection, largeDataSet);
        DatabaseTestUtils.commit(connection);
        long start = System.currentTimeMillis();
        // Example: count users, products, orders, order_items, reviews
        int userCount = 0, productCount = 0, orderCount = 0, orderItemCount = 0, reviewCount = 0;
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "SELECT (SELECT COUNT(*) FROM users), (SELECT COUNT(*) FROM products), (SELECT COUNT(*) FROM orders), (SELECT COUNT(*) FROM order_items), (SELECT COUNT(*) FROM reviews)")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userCount = rs.getInt(1);
                productCount = rs.getInt(2);
                orderCount = rs.getInt(3);
                orderItemCount = rs.getInt(4);
                reviewCount = rs.getInt(5);
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        System.out.println("Query performance: " + elapsed + " ms (users=" + userCount + ", products=" + productCount
                + ", orders=" + orderCount + ", order_items=" + orderItemCount + ", reviews=" + reviewCount + ")");
        // Assert query within 10 seconds
        assertTrue(elapsed < 10_000, "Query should complete within 10 seconds, actual: " + elapsed + " ms");
    }
}
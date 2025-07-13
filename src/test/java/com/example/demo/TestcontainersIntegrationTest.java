package com.example.demo;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test using Testcontainers to demonstrate a complete isolated database testing environment.
 * This test creates a temporary MySQL container, runs migrations, and tests with generated data.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestcontainersIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(TestcontainersIntegrationTest.class);
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testcontainer_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withInitScript("db/init/01-create-schema.sql");
    
    private IDatabaseConnection connection;
    private TestDataGenerator dataGenerator;
    
    @BeforeAll
    static void beforeAll() {
        logger.info("Starting MySQL testcontainer...");
        mysql.start();
        logger.info("MySQL testcontainer started at: {}", mysql.getJdbcUrl());
    }
    
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up Testcontainers integration test");
        
        // Create connection to testcontainer database
        Connection jdbcConnection = DriverManager.getConnection(
                mysql.getJdbcUrl(), 
                mysql.getUsername(), 
                mysql.getPassword());
        
        jdbcConnection.setAutoCommit(false);
        
        connection = new DatabaseConnection(jdbcConnection);
        
        // Configure DBUnit for MySQL
        connection.getConfig().setProperty("http://www.dbunit.org/properties/datatypeFactory", 
                new org.dbunit.ext.mysql.MySqlDataTypeFactory());
        connection.getConfig().setProperty("http://www.dbunit.org/properties/metadataHandler", 
                new org.dbunit.ext.mysql.MySqlMetadataHandler());
        
        // Create data generator
        dataGenerator = new TestDataGenerator(54321L); // Different seed for variety
        
        logger.info("Testcontainers integration test setup completed");
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                logger.info("Cleaning up testcontainer data");
                
                // Disable foreign key checks temporarily
                try (PreparedStatement stmt = connection.getConnection()
                        .prepareStatement("SET FOREIGN_KEY_CHECKS = 0")) {
                    stmt.executeUpdate();
                }
                
                // Truncate all tables (faster than DELETE)
                String[] tables = {"reviews", "order_items", "orders", "products", "users"};
                for (String table : tables) {
                    try (PreparedStatement stmt = connection.getConnection()
                            .prepareStatement("TRUNCATE TABLE " + table)) {
                        stmt.executeUpdate();
                    }
                }
                
                // Re-enable foreign key checks
                try (PreparedStatement stmt = connection.getConnection()
                        .prepareStatement("SET FOREIGN_KEY_CHECKS = 1")) {
                    stmt.executeUpdate();
                }
                
                connection.getConnection().commit();
                logger.info("Testcontainer cleanup completed");
            } finally {
                connection.close();
            }
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Test Testcontainer Database Connection")
    void testTestcontainerConnection() throws Exception {
        logger.info("Testing testcontainer database connection");
        
        assertNotNull(connection, "Database connection should not be null");
        assertNotNull(connection.getConnection(), "JDBC connection should not be null");
        assertFalse(connection.getConnection().isClosed(), "Connection should be open");
        
        // Test basic database operations
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT 1 as test_value")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("test_value"));
        }
        
        logger.info("Testcontainer database connection test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Schema Creation in Testcontainer")
    void testSchemaCreation() throws Exception {
        logger.info("Testing schema creation in testcontainer");
        
        // Check if all expected tables exist
        String[] expectedTables = {"users", "products", "orders", "order_items", "reviews"};
        
        for (String tableName : expectedTables) {
            try (PreparedStatement stmt = connection.getConnection()
                    .prepareStatement("SELECT COUNT(*) FROM information_schema.tables " +
                            "WHERE table_schema = ? AND table_name = ?")) {
                stmt.setString(1, mysql.getDatabaseName());
                stmt.setString(2, tableName);
                
                ResultSet rs = stmt.executeQuery();
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Table " + tableName + " should exist");
            }
        }
        
        logger.info("Schema creation test passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test Complete Data Workflow in Testcontainer")
    void testCompleteDataWorkflow() throws Exception {
        logger.info("Testing complete data workflow in testcontainer");
        
        // Generate and insert data
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(30, 50, 25);
        DatabaseTestUtils.setupTestData(connection, dataSet);
        connection.getConnection().commit();
        
        // Verify data integrity and relationships
        
        // 1. Check user count
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(30, rs.getInt(1), "Should have 30 users");
        }
        
        // 2. Check product count and categories
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) as total, COUNT(DISTINCT category) as categories FROM products")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(50, rs.getInt("total"), "Should have 50 products");
            assertTrue(rs.getInt("categories") > 1, "Should have multiple categories");
        }
        
        // 3. Check order relationships
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM orders")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(25, rs.getInt(1), "Should have 25 orders");
        }
        
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM order_items")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt(1) >= 25, "Should have at least 25 order items");
        }
        
        // 4. Check foreign key constraints
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement(
                    "SELECT " +
                    "(SELECT COUNT(*) FROM orders WHERE user_id NOT IN (SELECT id FROM users)) as orphan_orders, " +
                    "(SELECT COUNT(*) FROM order_items WHERE order_id NOT IN (SELECT id FROM orders)) as orphan_items, " +
                    "(SELECT COUNT(*) FROM order_items WHERE product_id NOT IN (SELECT id FROM products)) as invalid_products")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt("orphan_orders"), "No orphan orders should exist");
            assertEquals(0, rs.getInt("orphan_items"), "No orphan order items should exist");
            assertEquals(0, rs.getInt("invalid_products"), "All order items should reference valid products");
        }
        
        logger.info("Complete data workflow test passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Complex Queries with Generated Data")
    void testComplexQueriesWithGeneratedData() throws Exception {
        logger.info("Testing complex queries with generated data");
        
        // Generate and insert data
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(20, 30, 15);
        DatabaseTestUtils.setupTestData(connection, dataSet);
        connection.getConnection().commit();
        
        // Test complex analytical queries
        
        // 1. Top customers by order value
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement(
                    "SELECT u.username, SUM(o.total_amount) as total_spent " +
                    "FROM users u " +
                    "JOIN orders o ON u.id = o.user_id " +
                    "GROUP BY u.id, u.username " +
                    "ORDER BY total_spent DESC " +
                    "LIMIT 5")) {
            ResultSet rs = stmt.executeQuery();
            
            int customerCount = 0;
            double previousTotal = Double.MAX_VALUE;
            
            while (rs.next()) {
                customerCount++;
                double currentTotal = rs.getDouble("total_spent");
                assertTrue(currentTotal <= previousTotal, "Results should be ordered by total spent");
                assertTrue(currentTotal > 0, "Total spent should be positive");
                previousTotal = currentTotal;
            }
            
            assertTrue(customerCount > 0, "Should have at least one customer with orders");
        }
        
        // 2. Product performance analysis
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement(
                    "SELECT p.category, " +
                    "COUNT(DISTINCT oi.order_id) as orders_count, " +
                    "SUM(oi.quantity) as total_quantity, " +
                    "AVG(oi.unit_price) as avg_price " +
                    "FROM products p " +
                    "JOIN order_items oi ON p.id = oi.product_id " +
                    "GROUP BY p.category " +
                    "ORDER BY orders_count DESC")) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                assertTrue(rs.getInt("orders_count") > 0, "Orders count should be positive");
                assertTrue(rs.getInt("total_quantity") > 0, "Total quantity should be positive");
                assertTrue(rs.getDouble("avg_price") > 0, "Average price should be positive");
            }
        }
        
        // 3. Order status distribution
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement(
                    "SELECT status, COUNT(*) as count, " +
                    "ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM orders), 2) as percentage " +
                    "FROM orders " +
                    "GROUP BY status " +
                    "ORDER BY count DESC")) {
            ResultSet rs = stmt.executeQuery();
            
            double totalPercentage = 0;
            while (rs.next()) {
                assertTrue(rs.getInt("count") > 0, "Count should be positive");
                double percentage = rs.getDouble("percentage");
                assertTrue(percentage > 0 && percentage <= 100, "Percentage should be between 0 and 100");
                totalPercentage += percentage;
            }
            
            assertEquals(100.0, totalPercentage, 0.1, "Total percentage should be 100%");
        }
        
        logger.info("Complex queries test passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Performance with Large Dataset in Testcontainer")
    void testPerformanceWithLargeDataset() throws Exception {
        logger.info("Testing performance with large dataset in testcontainer");
        
        long startTime = System.currentTimeMillis();
        
        // Generate large dataset
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(100, 150, 80);
        
        long generationTime = System.currentTimeMillis() - startTime;
        logger.info("Large dataset generation took {} ms", generationTime);
        
        // Insert data
        startTime = System.currentTimeMillis();
        DatabaseTestUtils.setupTestData(connection, dataSet);
        connection.getConnection().commit();
        
        long insertionTime = System.currentTimeMillis() - startTime;
        logger.info("Large dataset insertion took {} ms", insertionTime);
        
        // Test query performance
        startTime = System.currentTimeMillis();
        
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement(
                    "SELECT u.username, COUNT(o.id) as order_count, " +
                    "SUM(o.total_amount) as total_spent, " +
                    "AVG(r.rating) as avg_rating " +
                    "FROM users u " +
                    "LEFT JOIN orders o ON u.id = o.user_id " +
                    "LEFT JOIN reviews r ON u.id = r.user_id " +
                    "GROUP BY u.id, u.username " +
                    "HAVING order_count > 0 " +
                    "ORDER BY total_spent DESC " +
                    "LIMIT 10")) {
            
            ResultSet rs = stmt.executeQuery();
            int resultCount = 0;
            while (rs.next()) {
                resultCount++;
                assertNotNull(rs.getString("username"));
                assertTrue(rs.getInt("order_count") > 0);
                assertTrue(rs.getDouble("total_spent") > 0);
            }
            assertTrue(resultCount > 0, "Should return some results");
        }
        
        long queryTime = System.currentTimeMillis() - startTime;
        logger.info("Complex query execution took {} ms", queryTime);
        
        // Performance assertions (these may need adjustment based on system performance)
        assertTrue(generationTime < 15000, "Large dataset generation should complete within 15 seconds");
        assertTrue(insertionTime < 45000, "Large dataset insertion should complete within 45 seconds");
        assertTrue(queryTime < 5000, "Complex query should complete within 5 seconds");
        
        logger.info("Performance test with large dataset passed");
    }
} 
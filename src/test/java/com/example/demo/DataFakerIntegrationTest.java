package com.example.demo;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced test class demonstrating DataFaker integration with DBUnit.
 * Shows how to generate large amounts of realistic test data for comprehensive testing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataFakerIntegrationTest {
    
    private static final Logger logger = LoggerFactory.getLogger(DataFakerIntegrationTest.class);
    
    private IDatabaseConnection connection;
    private TestDataGenerator dataGenerator;
    
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up DataFaker integration test");
        
        // Create database connection
        connection = DatabaseTestUtils.createDatabaseConnection();
        
        // Create data generator with fixed seed for reproducible tests
        dataGenerator = new TestDataGenerator(12345L);
        
        logger.info("DataFaker integration test setup completed");
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                logger.info("Cleaning up test data");
                // Truncate all tables to clean up
                String[] tables = {"reviews", "order_items", "orders", "products", "users"};
                for (String table : tables) {
                    try (PreparedStatement stmt = connection.getConnection()
                            .prepareStatement("DELETE FROM " + table)) {
                        stmt.executeUpdate();
                    }
                }
                DatabaseTestUtils.commit(connection);
                logger.info("Test cleanup completed");
            } finally {
                DatabaseTestUtils.closeConnection(connection);
            }
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("Test Small Dataset Generation")
    void testSmallDatasetGeneration() throws Exception {
        logger.info("Testing small dataset generation");
        
        // Generate small dataset
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(5, 8, 3);
        
        // Setup data in database
        DatabaseTestUtils.setupTestData(connection, dataSet);
        DatabaseTestUtils.commit(connection);
        
        // Verify data was inserted correctly
        ITable usersTable = dataSet.getTable("users");
        ITable productsTable = dataSet.getTable("products");
        ITable ordersTable = dataSet.getTable("orders");
        
        assertEquals(5, usersTable.getRowCount(), "Should have 5 users");
        assertEquals(8, productsTable.getRowCount(), "Should have 8 products");
        assertEquals(3, ordersTable.getRowCount(), "Should have 3 orders");
        
        // Verify data in database
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(5, rs.getInt(1), "Database should contain 5 users");
        }
        
        logger.info("Small dataset generation test passed");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test Medium Dataset Generation")
    void testMediumDatasetGeneration() throws Exception {
        logger.info("Testing medium dataset generation");
        
        // Generate medium dataset
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(50, 100, 75);
        
        // Setup data in database
        DatabaseTestUtils.setupTestData(connection, dataSet);
        DatabaseTestUtils.commit(connection);
        
        // Verify counts
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(50, rs.getInt(1), "Database should contain 50 users");
        }
        
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM products")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(100, rs.getInt(1), "Database should contain 100 products");
        }
        
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM orders")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(75, rs.getInt(1), "Database should contain 75 orders");
        }
        
        logger.info("Medium dataset generation test passed");
    }
    
    @Test
    @Order(3)
    @DisplayName("Test Data Uniqueness and Integrity")
    void testDataUniquenessAndIntegrity() throws Exception {
        logger.info("Testing data uniqueness and integrity");
        
        // Generate dataset
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(20, 30, 15);
        DatabaseTestUtils.setupTestData(connection, dataSet);
        DatabaseTestUtils.commit(connection);
        
        // Test username uniqueness
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT username, COUNT(*) as count FROM users GROUP BY username HAVING COUNT(*) > 1")) {
            ResultSet rs = stmt.executeQuery();
            assertFalse(rs.next(), "All usernames should be unique");
        }
        
        // Test email uniqueness
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT email, COUNT(*) as count FROM users GROUP BY email HAVING COUNT(*) > 1")) {
            ResultSet rs = stmt.executeQuery();
            assertFalse(rs.next(), "All emails should be unique");
        }
        
        // Test SKU uniqueness
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT sku, COUNT(*) as count FROM products GROUP BY sku HAVING COUNT(*) > 1")) {
            ResultSet rs = stmt.executeQuery();
            assertFalse(rs.next(), "All SKUs should be unique");
        }
        
        // Test order number uniqueness
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT order_number, COUNT(*) as count FROM orders GROUP BY order_number HAVING COUNT(*) > 1")) {
            ResultSet rs = stmt.executeQuery();
            assertFalse(rs.next(), "All order numbers should be unique");
        }
        
        // Test foreign key integrity
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM orders o LEFT JOIN users u ON o.user_id = u.id WHERE u.id IS NULL")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "All orders should reference valid users");
        }
        
        logger.info("Data uniqueness and integrity test passed");
    }
    
    @Test
    @Order(4)
    @DisplayName("Test Data Realism and Variety")
    void testDataRealismAndVariety() throws Exception {
        logger.info("Testing data realism and variety");
        
        // Generate dataset
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(25, 40, 20);
        DatabaseTestUtils.setupTestData(connection, dataSet);
        DatabaseTestUtils.commit(connection);
        
        // Check email format variety
        Set<String> emailDomains = new HashSet<>();
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT SUBSTRING_INDEX(email, '@', -1) as domain FROM users")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                emailDomains.add(rs.getString("domain"));
            }
        }
        assertTrue(emailDomains.size() > 1, "Should have variety in email domains");
        
        // Check product categories
        Set<String> categories = new HashSet<>();
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT DISTINCT category FROM products")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }
        assertTrue(categories.size() > 2, "Should have multiple product categories");
        
        // Check price range
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT MIN(price) as min_price, MAX(price) as max_price FROM products")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            double minPrice = rs.getDouble("min_price");
            double maxPrice = rs.getDouble("max_price");
            assertTrue(minPrice > 0, "Minimum price should be positive");
            assertTrue(maxPrice > minPrice * 2, "Should have good price range variety");
        }
        
        // Check order status variety
        Set<String> orderStatuses = new HashSet<>();
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT DISTINCT status FROM orders")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                orderStatuses.add(rs.getString("status"));
            }
        }
        assertTrue(orderStatuses.size() > 1, "Should have variety in order statuses");
        
        // Check review ratings variety
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(DISTINCT rating) as rating_variety FROM reviews")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertTrue(rs.getInt("rating_variety") > 2, "Should have variety in review ratings");
        }
        
        logger.info("Data realism and variety test passed");
    }
    
    @Test
    @Order(5)
    @DisplayName("Test Large Dataset Performance")
    void testLargeDatasetPerformance() throws Exception {
        logger.info("Testing large dataset performance");
        
        long startTime = System.currentTimeMillis();
        
        // Generate large dataset
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(100, 200, 150);
        
        long generationTime = System.currentTimeMillis() - startTime;
        logger.info("Dataset generation took {} ms", generationTime);
        
        // Insert data
        startTime = System.currentTimeMillis();
        DatabaseTestUtils.setupTestData(connection, dataSet);
        DatabaseTestUtils.commit(connection);
        
        long insertionTime = System.currentTimeMillis() - startTime;
        logger.info("Data insertion took {} ms", insertionTime);
        
        // Verify counts
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT " +
                    "(SELECT COUNT(*) FROM users) as user_count, " +
                    "(SELECT COUNT(*) FROM products) as product_count, " +
                    "(SELECT COUNT(*) FROM orders) as order_count, " +
                    "(SELECT COUNT(*) FROM order_items) as order_item_count, " +
                    "(SELECT COUNT(*) FROM reviews) as review_count")) {
            
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            
            assertEquals(100, rs.getInt("user_count"));
            assertEquals(200, rs.getInt("product_count"));
            assertEquals(150, rs.getInt("order_count"));
            assertTrue(rs.getInt("order_item_count") >= 150); // At least one item per order
            assertTrue(rs.getInt("review_count") > 0);
            
            logger.info("Final counts - Users: {}, Products: {}, Orders: {}, Order Items: {}, Reviews: {}",
                    rs.getInt("user_count"), rs.getInt("product_count"), 
                    rs.getInt("order_count"), rs.getInt("order_item_count"), 
                    rs.getInt("review_count"));
        }
        
        // Performance assertions
        assertTrue(generationTime < 10000, "Dataset generation should complete within 10 seconds");
        assertTrue(insertionTime < 30000, "Data insertion should complete within 30 seconds");
        
        logger.info("Large dataset performance test passed");
    }
    
    @Test
    @Order(6)
    @DisplayName("Test Individual Table Generation")
    void testIndividualTableGeneration() throws Exception {
        logger.info("Testing individual table generation");
        
        // Test users table generation only
        IDataSet usersDataSet = new org.dbunit.dataset.DefaultDataSet();
        ((org.dbunit.dataset.DefaultDataSet) usersDataSet)
                .addTable(dataGenerator.generateUsersTable(10));
        
        DatabaseTestUtils.setupTestData(connection, usersDataSet);
        DatabaseTestUtils.commit(connection);
        
        // Verify users were created
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM users")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(10, rs.getInt(1), "Should have 10 users");
        }
        
        // Clean up
        DatabaseTestUtils.cleanupTestData(connection, usersDataSet);
        DatabaseTestUtils.commit(connection);
        
        // Test products table generation only
        IDataSet productsDataSet = new org.dbunit.dataset.DefaultDataSet();
        ((org.dbunit.dataset.DefaultDataSet) productsDataSet)
                .addTable(dataGenerator.generateProductsTable(15));
        
        DatabaseTestUtils.setupTestData(connection, productsDataSet);
        DatabaseTestUtils.commit(connection);
        
        // Verify products were created
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM products")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(15, rs.getInt(1), "Should have 15 products");
        }
        
        logger.info("Individual table generation test passed");
    }
} 
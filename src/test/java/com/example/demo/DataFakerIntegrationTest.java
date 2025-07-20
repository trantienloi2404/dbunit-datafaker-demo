package com.example.demo;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.DefaultTable;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test class for DataFaker functionality.
 * Tests realistic data generation and database integration.
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
                logger.info("Cleaning up DataFaker test data");
                // Truncate all tables for cleanup
                String[] tables = {"reviews", "order_items", "orders", "products", "users"};
                for (String table : tables) {
                    try (PreparedStatement stmt = connection.getConnection()
                            .prepareStatement("DELETE FROM " + table)) {
                        stmt.executeUpdate();
                    }
                }
                DatabaseTestUtils.commit(connection);
                logger.info("DataFaker test cleanup completed");
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
    @DisplayName("Test Data Uniqueness and Integrity")
    void testDataUniquenessAndIntegrity() throws Exception {
        logger.info("Testing data uniqueness and integrity");
        
        // Generate dataset with potential for duplicates
        IDataSet dataSet = dataGenerator.generateCompleteDataSet(10, 15, 8);
        
        // Setup data in database
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
        
        // Test product SKU uniqueness
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT sku, COUNT(*) as count FROM products GROUP BY sku HAVING COUNT(*) > 1")) {
            ResultSet rs = stmt.executeQuery();
            assertFalse(rs.next(), "All product SKUs should be unique");
        }
        
        // Test foreign key integrity
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT COUNT(*) FROM orders o LEFT JOIN users u ON o.user_id = u.id WHERE u.id IS NULL")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1), "All orders should have valid user references");
        }
        
        logger.info("Data uniqueness and integrity test passed");
    }

    @Test
    @Order(3)
    @DisplayName("Test Individual Table Generation")
    void testIndividualTableGeneration() throws Exception {
        logger.info("Testing individual table generation");
        
        // Test individual table generation methods
        DefaultTable usersTable = dataGenerator.generateUsersTable(5);
        DefaultTable productsTable = dataGenerator.generateProductsTable(8);
        
        // Verify individual tables
        assertEquals(5, usersTable.getRowCount(), "Users table should have 5 rows");
        assertEquals(8, productsTable.getRowCount(), "Products table should have 8 rows");
        
        // Check that generated data has required fields
        for (int i = 0; i < usersTable.getRowCount(); i++) {
            assertNotNull(usersTable.getValue(i, "username"), "Username should not be null");
            assertNotNull(usersTable.getValue(i, "email"), "Email should not be null");
            assertNotNull(usersTable.getValue(i, "first_name"), "First name should not be null");
            assertNotNull(usersTable.getValue(i, "last_name"), "Last name should not be null");
        }
        
        for (int i = 0; i < productsTable.getRowCount(); i++) {
            assertNotNull(productsTable.getValue(i, "name"), "Product name should not be null");
            assertNotNull(productsTable.getValue(i, "category"), "Product category should not be null");
            assertNotNull(productsTable.getValue(i, "sku"), "Product SKU should not be null");
            assertNotNull(productsTable.getValue(i, "price"), "Product price should not be null");
        }
        
        logger.info("Individual table generation test passed");
    }
} 
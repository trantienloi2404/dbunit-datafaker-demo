package com.example.demo;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional test class for testing stored procedures, functions, and triggers.
 * This demonstrates functional testing of database business logic as required
 * for comprehensive database testing coverage.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseFunctionalTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseFunctionalTest.class);
    
    private IDatabaseConnection connection;
    private TestDataGenerator dataGenerator;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up functional database test");

        // Create database connection
        connection = DatabaseTestUtils.createDatabaseConnection();
        
        // Create data generator for test data
        dataGenerator = new TestDataGenerator(12345L);

        // Generate and setup test data
        IDataSet testDataSet = dataGenerator.generateCompleteDataSet(5, 10, 3);
        DatabaseTestUtils.setupTestData(connection, testDataSet);
        DatabaseTestUtils.commit(connection);

        logger.info("Functional test setup completed");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                logger.info("Cleaning up functional test data");
                // Clean up all test data
                String[] tables = {"reviews", "order_items", "orders", "products", "users"};
                for (String table : tables) {
                    try (PreparedStatement stmt = connection.getConnection()
                            .prepareStatement("DELETE FROM " + table)) {
                        stmt.executeUpdate();
                    }
                }
                DatabaseTestUtils.commit(connection);
                logger.info("Functional test cleanup completed");
            } finally {
                DatabaseTestUtils.closeConnection(connection);
            }
        }
    }

    // ===============================
    // FUNCTION TESTING
    // ===============================

    @Test
    @Order(1)
    @DisplayName("Test calculate_order_total Function")
    void testCalculateOrderTotalFunction() throws Exception {
        logger.info("Testing calculate_order_total function");

        // Test with default tax rate (8.75%)
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT calculate_order_total(1) as total_with_tax")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Function should return a result");
            
            BigDecimal totalWithTax = rs.getBigDecimal("total_with_tax");
            assertNotNull(totalWithTax, "Total with tax should not be null");
            assertTrue(totalWithTax.compareTo(BigDecimal.ZERO) > 0, 
                    "Total with tax should be positive");
            
            logger.info("Order 1 total with default tax: {}", totalWithTax);
        }

        // Test with custom tax rate (10%) using the separate function
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT calculate_order_total_with_tax(1, 0.10) as total_with_custom_tax")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Function should return a result");
            
            BigDecimal totalWithCustomTax = rs.getBigDecimal("total_with_custom_tax");
            assertNotNull(totalWithCustomTax, "Total with custom tax should not be null");
            
            logger.info("Order 1 total with 10% tax: {}", totalWithCustomTax);
        }

        logger.info("calculate_order_total function test passed");
    }

    // ===============================
    // STORED PROCEDURE TESTING
    // ===============================

    @Test
    @Order(2)
    @DisplayName("Test process_order_shipment Stored Procedure")
    void testProcessOrderShipmentProcedure() throws Exception {
        logger.info("Testing process_order_shipment stored procedure");

        // First, ensure we have an order in PENDING status
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("UPDATE orders SET status = 'PENDING' WHERE id = 1")) {
            stmt.executeUpdate();
            DatabaseTestUtils.commit(connection);
        }

        // Test successful order shipment
        try (CallableStatement stmt = connection.getConnection()
                .prepareCall("CALL process_order_shipment(?, ?)")) {
            stmt.setLong(1, 1L);
            stmt.setString(2, "TRACK123456");
            
            boolean hasResults = stmt.execute();
            assertTrue(hasResults, "Procedure should return results");
            
            ResultSet rs = stmt.getResultSet();
            assertTrue(rs.next(), "Procedure should return a result row");
            
            String result = rs.getString("result");
            assertNotNull(result, "Result should not be null");
            assertTrue(result.contains("Order 1 shipped"), "Result should confirm shipment");
            assertTrue(result.contains("TRACK123456"), "Result should include tracking number");
            
            logger.info("Shipment result: {}", result);
        }

        // Verify order status was updated
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT status, shipped_date FROM orders WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Order should exist");
            
            String status = rs.getString("status");
            assertEquals("SHIPPED", status, "Order status should be updated to SHIPPED");
            
            assertNotNull(rs.getTimestamp("shipped_date"), "Shipped date should be set");
            
            logger.info("Order 1 status after shipment: {}", status);
        }

        logger.info("process_order_shipment procedure test passed");
    }

    // ===============================
    // TRIGGER TESTING
    // ===============================

    @Test
    @Order(3)
    @DisplayName("Test Stock Update Triggers")
    void testStockUpdateTriggers() throws Exception {
        logger.info("Testing stock update triggers");

        // Get initial product stock
        int initialStock;
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT stock_quantity FROM products WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Product should exist");
            initialStock = rs.getInt("stock_quantity");
            logger.info("Initial stock for product 1: {}", initialStock);
        }

        // Insert order item to test stock decrease trigger
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement(
                    "INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price) " +
                    "VALUES (1, 1, 5, 100.00, 500.00)")) {
            stmt.executeUpdate();
            DatabaseTestUtils.commit(connection);
        }

        // Verify stock was decreased by trigger
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT stock_quantity, is_available FROM products WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Product should exist");
            
            int newStock = rs.getInt("stock_quantity");
            assertEquals(initialStock - 5, newStock, 
                    "Stock should be decreased by quantity ordered");
            
            logger.info("Stock after order: {} (decreased by 5)", newStock);
        }

        // Test deletion trigger (order cancellation)
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement(
                    "DELETE FROM order_items WHERE order_id = 1 AND product_id = 1 AND quantity = 5")) {
            int deletedRows = stmt.executeUpdate();
            assertEquals(1, deletedRows, "Should delete one order item");
            DatabaseTestUtils.commit(connection);
        }

        // Verify stock was restored by trigger
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT stock_quantity, is_available FROM products WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            assertTrue(rs.next(), "Product should exist");
            
            int restoredStock = rs.getInt("stock_quantity");
            assertEquals(initialStock, restoredStock, 
                    "Stock should be restored after order cancellation");
            
            boolean isAvailable = rs.getBoolean("is_available");
            assertTrue(isAvailable, "Product should be available after stock restoration");
            
            logger.info("Stock after cancellation: {} (restored)", restoredStock);
        }

        logger.info("Stock update triggers test passed");
    }
} 
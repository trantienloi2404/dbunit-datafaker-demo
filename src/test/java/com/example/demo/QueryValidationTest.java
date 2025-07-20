package com.example.demo;

import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Query validation test class that validates developer queries are syntactically
 * and logically correct according to business requirements. This demonstrates
 * functional testing of database queries as requested by the instructor.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(QueryValidationTest.class);
    
    private IDatabaseConnection connection;
    private TestDataGenerator dataGenerator;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up query validation test");

        // Create database connection
        connection = DatabaseTestUtils.createDatabaseConnection();
        
        // Create data generator for test data
        dataGenerator = new TestDataGenerator(12345L);

        // Generate and setup comprehensive test data
        IDataSet testDataSet = dataGenerator.generateCompleteDataSet(10, 20, 15);
        DatabaseTestUtils.setupTestData(connection, testDataSet);
        
        // Add additional test data for query validation
        setupAdditionalTestData();
        
        DatabaseTestUtils.commit(connection);

        logger.info("Query validation test setup completed");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            try {
                logger.info("Cleaning up query validation test data");
                String[] tables = {"reviews", "order_items", "orders", "products", "users"};
                for (String table : tables) {
                    try (PreparedStatement stmt = connection.getConnection()
                            .prepareStatement("DELETE FROM " + table)) {
                        stmt.executeUpdate();
                    }
                }
                DatabaseTestUtils.commit(connection);
                logger.info("Query validation test cleanup completed");
            } finally {
                DatabaseTestUtils.closeConnection(connection);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Validate User Authentication Query Logic")
    void validateUserAuthenticationQuery() throws SQLException {
        logger.info("Testing user authentication query logic");

        // Business Requirement: User authentication should only succeed for active users
        String authQuery = 
            "SELECT id, username, email, is_active " +
            "FROM users " +
            "WHERE (username = ? OR email = ?) AND is_active = TRUE";

        // Test with valid active user
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(authQuery)) {
            stmt.setString(1, getUsernameById(1));
            stmt.setString(2, getUsernameById(1)); // Using username for both parameters
            
            try (ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Active user should be found");
                assertTrue(rs.getBoolean("is_active"), "User should be active");
                assertFalse(rs.next(), "Should return exactly one user");
            }
        }

        // Deactivate a user and test that authentication fails
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("UPDATE users SET is_active = FALSE WHERE id = 2")) {
            stmt.executeUpdate();
        }

        // Test with inactive user
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(authQuery)) {
            stmt.setString(1, getUsernameById(2));
            stmt.setString(2, getUsernameById(2));
            
            try (ResultSet rs = stmt.executeQuery()) {
                assertFalse(rs.next(), "Inactive user should not be found for authentication");
            }
        }

        logger.info("User authentication query validation passed");
    }

    @Test
    @Order(2)
    @DisplayName("Validate Sales Report Query Logic")
    void validateSalesReportQuery() throws SQLException {
        logger.info("Testing sales report query logic");

        // Business Requirement: Sales reports should provide accurate aggregations
        String salesReportQuery = 
            "SELECT " +
            "    DATE(o.order_date) as order_date, " +
            "    COUNT(o.id) as total_orders, " +
            "    SUM(o.total_amount) as total_revenue, " +
            "    AVG(o.total_amount) as avg_order_value, " +
            "    COUNT(DISTINCT o.user_id) as unique_customers " +
            "FROM orders o " +
            "WHERE o.status IN ('DELIVERED', 'SHIPPED') " +
            "AND o.order_date >= DATE_SUB(CURRENT_DATE, INTERVAL 30 DAY) " +
            "GROUP BY DATE(o.order_date) " +
            "ORDER BY order_date DESC";

        try (PreparedStatement stmt = connection.getConnection().prepareStatement(salesReportQuery)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    java.sql.Date orderDate = rs.getDate("order_date");
                    int totalOrders = rs.getInt("total_orders");
                    BigDecimal totalRevenue = rs.getBigDecimal("total_revenue");
                    BigDecimal avgOrderValue = rs.getBigDecimal("avg_order_value");
                    int uniqueCustomers = rs.getInt("unique_customers");
                    
                    // Validate report data integrity
                    assertNotNull(orderDate, "Order date should not be null");
                    assertTrue(totalOrders > 0, "Total orders should be positive");
                    assertTrue(totalRevenue.compareTo(BigDecimal.ZERO) > 0, 
                            "Total revenue should be positive");
                    assertTrue(avgOrderValue.compareTo(BigDecimal.ZERO) > 0, 
                            "Average order value should be positive");
                    assertTrue(uniqueCustomers > 0, "Unique customers should be positive");
                    
                    // Validate business logic: avg = total / count
                    BigDecimal calculatedAvg = totalRevenue.divide(
                            new BigDecimal(totalOrders), 2, BigDecimal.ROUND_HALF_UP);
                    assertEquals(0, avgOrderValue.compareTo(calculatedAvg), 
                            "Average order value should match calculated average");
                    
                    logger.info("Sales report - Date: {} - Orders: {} - Revenue: {} - Avg: {} - Customers: {}", 
                            orderDate, totalOrders, totalRevenue, avgOrderValue, uniqueCustomers);
                }
            }
        }

        logger.info("Sales report query validation passed");
    }

    @Test
    @Order(3)
    @DisplayName("Validate Complex Join Query Logic")
    void validateComplexJoinQuery() throws SQLException {
        logger.info("Testing complex join query logic");

        // Business Requirement: Complex queries should maintain data integrity across joins
        String complexJoinQuery = 
            "SELECT " +
            "    p.name as product_name, " +
            "    p.category, " +
            "    COUNT(DISTINCT oi.order_id) as times_ordered, " +
            "    SUM(oi.quantity) as total_quantity_sold, " +
            "    SUM(oi.total_price) as total_revenue, " +
            "    COUNT(DISTINCT r.id) as total_reviews, " +
            "    COALESCE(AVG(r.rating), 0) as avg_rating, " +
            "    p.stock_quantity as current_stock " +
            "FROM products p " +
            "LEFT JOIN order_items oi ON p.id = oi.product_id " +
            "LEFT JOIN orders o ON oi.order_id = o.id AND o.status IN ('DELIVERED', 'SHIPPED') " +
            "LEFT JOIN reviews r ON p.id = r.product_id " +
            "GROUP BY p.id, p.name, p.category, p.stock_quantity " +
            "HAVING times_ordered > 0 OR total_reviews > 0 " +
            "ORDER BY total_revenue DESC";

        try (PreparedStatement stmt = connection.getConnection().prepareStatement(complexJoinQuery)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String productName = rs.getString("product_name");
                    String category = rs.getString("category");
                    int timesOrdered = rs.getInt("times_ordered");
                    int totalQuantitySold = rs.getInt("total_quantity_sold");
                    BigDecimal totalRevenue = rs.getBigDecimal("total_revenue");
                    int totalReviews = rs.getInt("total_reviews");
                    BigDecimal avgRating = rs.getBigDecimal("avg_rating");
                    int currentStock = rs.getInt("current_stock");
                    
                    // Validate complex query data integrity
                    assertNotNull(productName, "Product name should not be null");
                    assertNotNull(category, "Category should not be null");
                    assertTrue(timesOrdered >= 0, "Times ordered should be non-negative");
                    assertTrue(totalQuantitySold >= 0, "Total quantity sold should be non-negative");
                    
                    // Handle null revenue (for products with no sales)
                    BigDecimal revenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
                    assertTrue(revenue.compareTo(BigDecimal.ZERO) >= 0, 
                            "Total revenue should be non-negative");
                    
                    assertTrue(totalReviews >= 0, "Total reviews should be non-negative");
                    assertTrue(currentStock >= 0, "Current stock should be non-negative");
                    
                    // Business logic validation
                    if (totalReviews > 0) {
                        assertTrue(avgRating.compareTo(BigDecimal.ZERO) > 0 && 
                                  avgRating.compareTo(new BigDecimal("5.00")) <= 0,
                                "Average rating should be between 1 and 5 when reviews exist");
                    }
                    
                    logger.info("Product: {} - Category: {} - Orders: {} - Revenue: {} - Rating: {} ({} reviews)", 
                            productName, category, timesOrdered, revenue, avgRating, totalReviews);
                }
            }
        }

        logger.info("Complex join query validation passed");
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private void setupAdditionalTestData() throws SQLException {
        // Add some specific test data for query validation
        logger.info("Setting up additional test data for query validation");
        
        // Add orders with different statuses
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "INSERT INTO orders (user_id, order_number, total_amount, status, delivery_address) VALUES " +
                "(1, 'QUERY-TEST-001', 150.00, 'DELIVERED', '123 Query St'), " +
                "(2, 'QUERY-TEST-002', 75.50, 'SHIPPED', '456 Test Ave'), " +
                "(3, 'QUERY-TEST-003', 299.99, 'PENDING', '789 Validation Rd')")) {
            stmt.executeUpdate();
        }
        
        logger.info("Additional test data setup completed");
    }

    private String getUsernameById(long userId) throws SQLException {
        try (PreparedStatement stmt = connection.getConnection()
                .prepareStatement("SELECT username FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        }
        return null;
    }
} 
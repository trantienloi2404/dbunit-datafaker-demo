package com.example.demo;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
import com.example.utils.TestDataGenerator;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic database test using DAO layer instead of direct SQL access.
 * Demonstrates fundamental DAO functionality for database testing:
 * tests should interact with DAO methods, not raw SQL.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicDatabaseTest {

    // Logger for test information
    private static final Logger logger = LoggerFactory.getLogger(BasicDatabaseTest.class);

    // Database connection manager
    private DatabaseConnectionManager connectionManager;
    // Test data generator using DataFaker
    private TestDataGenerator dataGenerator;
    // Summary of generated test data
    private TestDataGenerator.TestDataSummary testDataSummary;
    
    // DAO instances to interact with the database
    private UserDao userDao;
    private ProductDao productDao;
    private OrderDao orderDao;
    private OrderItemDao orderItemDao;
    private ReviewDao reviewDao;

    /**
     * Setup before each test case: initialize connection, DAOs, and seed test data
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up basic database test");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();

        // Create data generator with fixed seed for reproducibility
        dataGenerator = new TestDataGenerator(12345L);

        // Generate test data via DAOs
        testDataSummary = dataGenerator.generateCompleteTestData(3, 5, 2);
        connectionManager.commit();

        logger.info("Finished setting up basic test");
    }

    /**
     * Cleanup after each test case: remove test data and close connection
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Cleaning up basic test data");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Finished cleaning up basic test");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    /**
     * Test database connection through the DAO layer
     * Checks that basic DAO operations can be executed
     */
    @Test
    @Order(1)
    @DisplayName("Test Database Connection Via DAO")
    void testDatabaseConnectionThroughDao() throws SQLException {
        logger.info("Testing database connection via DAO layer");
        
        // Exercise simple DAO operations
        long userCount = userDao.count();
        long productCount = productDao.count();
        long orderCount = orderDao.count();
        
        assertTrue(userCount >= 0, "User count must be non-negative");
        assertTrue(productCount >= 0, "Product count must be non-negative");
        assertTrue(orderCount >= 0, "Order count must be non-negative");
        
        logger.info("Database connection OK - Users: {}, Products: {}, Orders: {}", 
                userCount, productCount, orderCount);
    }

    /**
     * Test data creation via DAO methods
     * Verifies generated test data is correct
     */
    @Test
    @Order(2)
    @DisplayName("Test Data Creation Via DAO Methods")
    void testDataCreationThroughDao() throws SQLException {
        logger.info("Testing data creation via DAO methods");
        
        // Verify summary counts
        assertEquals(3, testDataSummary.getUserCount(), "There must be 3 users in test data");
        assertEquals(5, testDataSummary.getProductCount(), "There must be 5 products in test data");
        assertEquals(2, testDataSummary.getOrderCount(), "There must be 2 orders in test data");
        
        // Verify data exists via DAO methods
        List<UserDto> users = userDao.findAll();
        List<ProductDto> products = productDao.findAll();
        List<OrderDto> orders = orderDao.findAll();
        
        assertTrue(users.size() >= 3, "There must be at least 3 users");
        assertTrue(products.size() >= 5, "There must be at least 5 products");
        assertTrue(orders.size() >= 2, "There must be at least 2 orders");

        logger.info("Data creation test passed");
    }

    /**
     * Test basic DAO operations for all entities
     * Checks CRUD operations and special queries for all entities
     */
    @Test
    @Order(3)
    @DisplayName("Test Basic DAO Operations")
    void testBasicDaoOperations() throws SQLException {
        logger.info("Testing basic DAO operations for all entities");
        
        // User DAO
        Long userId = testDataSummary.getUserIds().get(0);
        UserDto user = userDao.findById(userId).orElseThrow();
        assertNotNull(user.getUsername(), "Username must not be null");
        assertTrue(user.getIsActive(), "User should be active by default");
        
        UserDto foundByUsername = userDao.findByUsername(user.getUsername()).orElseThrow();
        assertEquals(user.getId(), foundByUsername.getId(), "Should find the same user by username");
        
        // Product DAO
        Long productId = testDataSummary.getProductIds().get(0);
        ProductDto product = productDao.findById(productId).orElseThrow();
        assertNotNull(product.getName(), "Product name must not be null");
        assertTrue(product.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0, "Price must be positive");
        
        ProductDto foundBySku = productDao.findBySku(product.getSku()).orElseThrow();
        assertEquals(product.getId(), foundBySku.getId(), "Should find the same product by SKU");
        
        // Order DAO
        Long orderId = testDataSummary.getOrderIds().get(0);
        OrderDto order = orderDao.findById(orderId).orElseThrow();
        assertNotNull(order.getOrderNumber(), "Order number must not be null");
        assertTrue(order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0, "Order total must be positive");
        
        // OrderItem relationships
        List<OrderItemDto> orderItems = orderItemDao.findByOrderId(orderId);
        assertFalse(orderItems.isEmpty(), "Order must have at least one item");
        
        logger.info("Basic DAO operations test passed");
    }

    /**
     * Test data counts and consistency
     * Checks counting methods and data consistency
     */
    @Test
    @Order(4)
    @DisplayName("Test Data Counts And Consistency")
    void testDataCountsAndConsistency() throws SQLException {
        logger.info("Testing data counts and consistency");
        
        // Count methods
        long userCount = userDao.count();
        long activeUserCount = userDao.countActive();
        long productCount = productDao.count();
        long orderCount = orderDao.count();
        long orderItemCount = orderItemDao.count();
        
        // Sanity checks
        assertTrue(activeUserCount <= userCount, "Active users cannot exceed total users");
        assertTrue(orderItemCount >= orderCount, "There must be at least as many order items as orders");
        
        logger.info("Data counts - Users: {} (Active: {}), Products: {}, Orders: {}, OrderItems: {}", 
                userCount, activeUserCount, productCount, orderCount, orderItemCount);
        
        logger.info("Data counts and consistency test passed");
    }
}
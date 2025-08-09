package com.example.demo;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
import com.example.utils.TestDataGenerator;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Query validation tests to verify business logic via DAO methods.
 * Ensures the SQL queries encapsulated in the DAO layer are syntactically and
 * logically correct according to business requirements.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryValidationTest {

    // Logger for test information
    private static final Logger logger = LoggerFactory.getLogger(QueryValidationTest.class);
    
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
     * Setup before each test case: initialize connection, DAOs, and generate comprehensive test data
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up query validation tests");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();
        
        // Create generator with fixed seed for reproducibility
        dataGenerator = new TestDataGenerator(12345L);

        // Generate comprehensive test data via DAOs
        testDataSummary = dataGenerator.generateCompleteTestData(10, 20, 15);
        
        // Add additional test data for query validation
        setupAdditionalTestData();
        
        connectionManager.commit();

        logger.info("Finished setting up query validation tests");
    }

    /**
     * Cleanup after each test case: remove test data and close connection
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Cleaning up query validation test data");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Finished cleaning up query validation tests");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    /**
     * Validates user authentication query logic: only active users can authenticate
     */
    @Test
    @Order(1)
    @DisplayName("Validate User Authentication Query Logic")
    void validateUserAuthenticationQuery() throws SQLException {
        logger.info("Testing user authentication query logic");

        // Get one user from the generated data
        Long userId = testDataSummary.getUserIds().get(0);
        Optional<UserDto> userOpt = userDao.findById(userId);
        assertTrue(userOpt.isPresent(), "Test user must exist");
        UserDto user = userOpt.get();

        // Business rule: Only active users can authenticate
        Optional<UserDto> foundUser = userDao.findByUsernameOrEmail(user.getUsername());
        assertTrue(foundUser.isPresent(), "Active user must be found for authentication");
        assertTrue(foundUser.get().getIsActive(), "User must be active");
        assertEquals(user.getId(), foundUser.get().getId(), "Should return the correct user");

        // Deactivate: inactive user should not authenticate
        userDao.deactivate(userId);
        connectionManager.commit();

        Optional<UserDto> deactivatedUser = userDao.findByUsernameOrEmail(user.getUsername());
        assertFalse(deactivatedUser.isPresent(), "Inactive user must not be found for authentication");

        // Re-activate for cleanup
        userDao.activate(userId);
        connectionManager.commit();

        logger.info("User authentication query validation passed");
    }

    /**
     * Validates sales report query logic: provides correct aggregation
     */
    @Test
    @Order(2)
    @DisplayName("Validate Sales Report Query Logic")
    void validateSalesReportQuery() throws SQLException {
        logger.info("Testing sales report query logic");

        // Business rule: sales report must provide correct aggregation
        List<OrderDto> salesOrders = orderDao.findForSalesReport(30);
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrderCount = 0;
        
        for (OrderDto order : salesOrders) {
            // Only DELIVERED/SHIPPED orders should be included
            assertTrue(order.getStatus().equals("DELIVERED") || order.getStatus().equals("SHIPPED"),
                    "Sales report must only include delivered or shipped orders");
            
            // Sanity checks
            assertNotNull(order.getOrderDate(), "Order date must not be null");
            assertTrue(order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0, 
                    "Order total must be positive");
            
            totalRevenue = totalRevenue.add(order.getTotalAmount());
            totalOrderCount++;
        }
        
        // Verify aggregation
        if (totalOrderCount > 0) {
            BigDecimal averageOrderValue = totalRevenue.divide(
                    new BigDecimal(totalOrderCount), 2, BigDecimal.ROUND_HALF_UP);
            assertTrue(averageOrderValue.compareTo(BigDecimal.ZERO) > 0, 
                    "Average order value must be positive");
            
            logger.info("Sales report validated - Orders: {} - Revenue: {} - Average: {}", 
                    totalOrderCount, totalRevenue, averageOrderValue);
        }
        
        // DAO revenue function
        BigDecimal daoRevenue = orderDao.getTotalRevenue();
        assertTrue(daoRevenue.compareTo(BigDecimal.ZERO) >= 0, 
                "DAO total revenue must be non-negative");

        logger.info("Sales report query validation passed");
    }

    /**
     * Validates database function logic
     */
    @Test
    @Order(3)
    @DisplayName("Validate Database Function Logic")
    void validateDatabaseFunctionLogic() throws SQLException {
        logger.info("Testing database function logic");

        // Order total function
        Long orderId = testDataSummary.getOrderIds().get(0);
        BigDecimal orderTotal = orderDao.calculateOrderTotal(orderId);
        assertNotNull(orderTotal, "Order total function must return a value");
        assertTrue(orderTotal.compareTo(BigDecimal.ZERO) > 0, "Order total must be positive");

        // Custom tax function
        BigDecimal customTaxRate = new BigDecimal("0.10"); // 10%
        BigDecimal orderTotalWithTax = orderDao.calculateOrderTotalWithTax(orderId, customTaxRate);
        assertNotNull(orderTotalWithTax, "Custom tax function must return a value");
        assertTrue(orderTotalWithTax.compareTo(orderTotal) > 0, "Total with tax must exceed base total");

        // Loyalty status function
        Long userId = testDataSummary.getUserIds().get(0);
        String loyaltyStatus = orderDao.getUserLoyaltyStatus(userId);
        assertNotNull(loyaltyStatus, "Loyalty status must not be null");
        assertTrue(loyaltyStatus.matches("BRONZE|SILVER|GOLD|PLATINUM"), 
                "Loyalty status must be valid");

        logger.info("Database function logic validation passed");
    }

    // ===============================
    // SUPPORT METHODS
    // ===============================

    /**
     * Sets up additional test data for query validation.
     * Creates orders with specific statuses for testing.
     */
    private void setupAdditionalTestData() throws SQLException {
        logger.info("Setting up additional test data for query validation");
        
        // Create specific test orders with known statuses
        if (!testDataSummary.getUserIds().isEmpty()) {
            Long userId = testDataSummary.getUserIds().get(0);
            
            OrderDto deliveredOrder = new OrderDto();
            deliveredOrder.setUserId(userId);
            deliveredOrder.setOrderNumber("QUERY-TEST-001");
            deliveredOrder.setTotalAmount(new BigDecimal("150.00"));
            deliveredOrder.setStatus("DELIVERED");
            deliveredOrder.setDeliveryAddress("123 Query St");
            deliveredOrder.setShippedDate(Timestamp.valueOf(LocalDateTime.now().minusDays(5)));
            
            OrderDto shippedOrder = new OrderDto();
            shippedOrder.setUserId(userId);
            shippedOrder.setOrderNumber("QUERY-TEST-002");
            shippedOrder.setTotalAmount(new BigDecimal("75.50"));
            shippedOrder.setStatus("SHIPPED");
            shippedOrder.setDeliveryAddress("456 Test Ave");
            shippedOrder.setShippedDate(Timestamp.valueOf(LocalDateTime.now().minusDays(2)));
            
            OrderDto pendingOrder = new OrderDto();
            pendingOrder.setUserId(userId);
            pendingOrder.setOrderNumber("QUERY-TEST-003");
            pendingOrder.setTotalAmount(new BigDecimal("299.99"));
            pendingOrder.setStatus("PENDING");
            pendingOrder.setDeliveryAddress("789 Validation Rd");
            
            orderDao.create(deliveredOrder);
            orderDao.create(shippedOrder);
            orderDao.create(pendingOrder);
        }
        
        logger.info("Finished setting up additional test data");
    }
} 
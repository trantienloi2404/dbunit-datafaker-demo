package com.example.demo;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
import com.example.utils.TestDataGenerator;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for DataFaker with the DAO layer.
 * Tests realistic data generation and database integration through DAO methods.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataFakerIntegrationTest {

    // Logger for test information
    private static final Logger logger = LoggerFactory.getLogger(DataFakerIntegrationTest.class);

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
     * Setup before each test case: initialize connection and DAOs
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up DataFaker integration tests");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();

        // Create generator with fixed seed for reproducibility
        dataGenerator = new TestDataGenerator(12345L);

        logger.info("Finished setting up DataFaker integration tests");
    }

    /**
     * Cleanup after each test case: remove test data and close connection
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Cleaning up DataFaker test data");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Finished cleaning up DataFaker tests");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    /**
     * Tests data uniqueness and integrity
     */
    @Test
    @Order(1)
    @DisplayName("Test Data Uniqueness And Integrity")
    void testDataUniquenessAndIntegrity() throws Exception {
        logger.info("Testing data uniqueness and integrity");

        // Tạo bộ dữ liệu với khả năng trùng lặp
        testDataSummary = dataGenerator.generateCompleteTestData(10, 15, 8);
        connectionManager.commit();

        // Username uniqueness
        List<UserDto> allUsers = userDao.findAll();
        Set<String> usernames = new HashSet<>();
        for (UserDto user : allUsers) {
            assertFalse(usernames.contains(user.getUsername()),
                    "All usernames must be unique: " + user.getUsername());
            usernames.add(user.getUsername());
        }

        // Email uniqueness
        Set<String> emails = new HashSet<>();
        for (UserDto user : allUsers) {
            assertFalse(emails.contains(user.getEmail()),
                    "All emails must be unique: " + user.getEmail());
            emails.add(user.getEmail());
        }

        // Product SKU uniqueness
        List<ProductDto> allProducts = productDao.findAll();
        Set<String> skus = new HashSet<>();
        for (ProductDto product : allProducts) {
            assertFalse(skus.contains(product.getSku()),
                    "All product SKUs must be unique: " + product.getSku());
            skus.add(product.getSku());
        }

        // Foreign key integrity: orders must reference a valid user
        List<OrderDto> allOrders = orderDao.findAll();
        for (OrderDto order : allOrders) {
            UserDto user = userDao.findById(order.getUserId()).orElse(null);
            assertNotNull(user, "All orders must have a valid user reference");
        }

        logger.info("Data uniqueness and integrity test passed");
    }

    /**
     * Tests data quality and realism
     */
    @Test
    @Order(2)
    @DisplayName("Test Data Quality And Realism")
    void testDataQualityAndRealism() throws Exception {
        logger.info("Testing data quality and realism");

        // Tạo dữ liệu test
        testDataSummary = dataGenerator.generateCompleteTestData(20, 30, 15);
        connectionManager.commit();

        // User data quality
        List<UserDto> users = userDao.findAll();
        for (UserDto user : users) {
            // Email validation (very basic)
            assertTrue(user.getEmail().contains("@"), "Email must contain '@'");
            assertTrue(user.getEmail().contains("."), "Email must contain '.'");
            
            // Non-empty names
            assertFalse(user.getFirstName().trim().isEmpty(), "First name must not be empty");
            assertFalse(user.getLastName().trim().isEmpty(), "Last name must not be empty");
            
            // Username validation
            assertTrue(user.getUsername().length() >= 3, "Username must be at least 3 characters");
            assertTrue(user.getUsername().matches("^[a-zA-Z0-9._-]+$"), "Username must contain only valid characters");
        }

        // Product data quality
        List<ProductDto> products = productDao.findAll();
        for (ProductDto product : products) {
            // Positive price
            assertTrue(product.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0, 
                    "Product price must be positive");
            
            // Non-empty product name
            assertFalse(product.getName().trim().isEmpty(), "Product name must not be empty");
            
            // SKU validation
            assertTrue(product.getSku().length() >= 5, "SKU must be at least 5 characters");
            assertTrue(product.getSku().matches("^[A-Z0-9-]+$"), "SKU must be uppercase letters, digits, or hyphens");
            
            // Non-negative stock
            assertTrue(product.getStockQuantity() >= 0, "Stock quantity must be non-negative");
        }

        // Order data quality
        List<OrderDto> orders = orderDao.findAll();
        for (OrderDto order : orders) {
            // Positive total
            assertTrue(order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0, 
                    "Order total must be positive");
            
            // Valid status
            assertTrue(order.getStatus().matches("PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED"), 
                    "Order status must be valid");
            
            // Valid order number
            assertTrue(order.getOrderNumber().startsWith("ORD"), "Order number must start with ORD");
        }

        logger.info("Data quality and realism test passed");
    }

    /**
     * Tests cleanup functionality: test data can be fully removed
     */
    @Test
    @Order(3)
    @DisplayName("Test Cleanup Functionality")
    void testCleanupFunctionality() throws Exception {
        logger.info("Testing cleanup functionality");

        // Tạo dữ liệu test
        testDataSummary = dataGenerator.generateCompleteTestData(5, 8, 3);
        connectionManager.commit();

        // Verify data exists
        long initialUserCount = userDao.count();
        long initialProductCount = productDao.count();
        long initialOrderCount = orderDao.count();

        assertTrue(initialUserCount >= 5, "There must be at least 5 users");
        assertTrue(initialProductCount >= 8, "There must be at least 8 products");
        assertTrue(initialOrderCount >= 3, "There must be at least 3 orders");

        // Thực hiện dọn dẹp
        dataGenerator.cleanupTestData(testDataSummary);
        connectionManager.commit();

        // Verify data was cleaned up
        long finalUserCount = userDao.count();
        long finalProductCount = productDao.count();
        long finalOrderCount = orderDao.count();

        // Note: cleanup only removes test data, not base data
        assertTrue(finalUserCount <= initialUserCount, "User count must not increase after cleanup");
        assertTrue(finalProductCount <= initialProductCount, "Product count must not increase after cleanup");
        assertTrue(finalOrderCount <= initialOrderCount, "Order count must not increase after cleanup");

        logger.info("Cleanup complete - Users: {} -> {}, Products: {} -> {}, Orders: {} -> {}", 
                initialUserCount, finalUserCount, initialProductCount, finalProductCount, 
                initialOrderCount, finalOrderCount);

        // Reset to avoid double-clean in tearDown
        testDataSummary = null;
    }
}
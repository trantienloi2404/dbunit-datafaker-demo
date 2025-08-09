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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional tests for the database through DAO methods.
 * Demonstrates testing of business logic using the DAO/DTO pattern.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseFunctionalTest {

    // Logger for test information
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFunctionalTest.class);
    
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
        logger.info("Setting up database functional tests");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();
        
        // Create generator and seed test data
        dataGenerator = new TestDataGenerator(12345L);
        testDataSummary = dataGenerator.generateCompleteTestData(5, 10, 3);
        connectionManager.commit();

        logger.info("Finished setting up functional tests");
    }

    /**
     * Cleanup after each test case: remove test data and close connection
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Cleaning up functional test data");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Finished cleaning up functional tests");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    // ===============================
    // FUNCTIONAL TESTS
    // ===============================

    /**
     * Tests order total calculation functions (default and custom tax)
     */
    @Test
    @Order(1)
    @DisplayName("Test Order Total Calculation Functions")
    void testCalculateOrderTotalFunction() throws Exception {
        logger.info("Testing calculate_order_total functions");

        // Lấy một đơn hàng test
        Long orderId = testDataSummary.getOrderIds().get(0);
        
        // Default tax rate (8.75%)
        BigDecimal totalWithTax = orderDao.calculateOrderTotal(orderId);
        assertNotNull(totalWithTax, "Function must return a value");
        assertTrue(totalWithTax.compareTo(BigDecimal.ZERO) > 0, 
                "Total with tax must be positive");
        
        logger.info("Order {} total with default tax: {}", orderId, totalWithTax);

        // Custom tax rate (10%) using separate function
        BigDecimal customTaxRate = new BigDecimal("0.10"); // 10%
        BigDecimal totalWithCustomTax = orderDao.calculateOrderTotalWithTax(orderId, customTaxRate);
        assertNotNull(totalWithCustomTax, "Function must return a value");
        assertTrue(totalWithCustomTax.compareTo(BigDecimal.ZERO) > 0, 
                "Total with custom tax must be positive");
        
        logger.info("Order {} total with 10% tax: {}", orderId, totalWithCustomTax);

        logger.info("Order total calculation functions test passed");
    }

    /**
     * Tests product stock management: decrease and increase stock
     */
    @Test
    @Order(2)
    @DisplayName("Test Product Stock Management")
    void testProductStockManagement() throws SQLException {
        logger.info("Testing product stock management");

        // Pick a product
        Long productId = testDataSummary.getProductIds().get(0);
        ProductDto product = productDao.findById(productId).orElseThrow();
        int initialStock = product.getStockQuantity();
        
        logger.info("Initial stock for product {}: {}", productId, initialStock);

        // Decrease stock
        int reductionAmount = 5;
        productDao.reduceStock(productId, reductionAmount);
        connectionManager.commit();
        
        ProductDto updatedProduct = productDao.findById(productId).orElseThrow();
        assertEquals(initialStock - reductionAmount, updatedProduct.getStockQuantity(), 
                "Stock must decrease correctly");
        
        logger.info("Stock after decrease: {}", updatedProduct.getStockQuantity());

        // Increase stock
        int increaseAmount = 3;
        productDao.increaseStock(productId, increaseAmount);
        connectionManager.commit();
        
        ProductDto finalProduct = productDao.findById(productId).orElseThrow();
        assertEquals(initialStock - reductionAmount + increaseAmount, finalProduct.getStockQuantity(), 
                "Stock must increase correctly");
        
        logger.info("Stock after increase: {}", finalProduct.getStockQuantity());

        logger.info("Product stock management test passed");
    }

    /**
     * Tests order status workflow (PENDING -> CONFIRMED -> SHIPPED -> DELIVERED)
     */
    @Test
    @Order(3)
    @DisplayName("Test Order Status Workflow")
    void testOrderStatusWorkflow() throws SQLException {
        logger.info("Testing order status workflow");

        // Pick an order
        Long orderId = testDataSummary.getOrderIds().get(0);
        OrderDto order = orderDao.findById(orderId).orElseThrow();
        
        logger.info("Initial status of order {}: {}", orderId, order.getStatus());

        // Update in workflow: PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
        
        // CONFIRMED
        orderDao.updateStatus(orderId, "CONFIRMED");
        connectionManager.commit();
        
        OrderDto confirmedOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("CONFIRMED", confirmedOrder.getStatus(), "Status must update to CONFIRMED");
        
        // SHIPPED
        orderDao.updateStatus(orderId, "SHIPPED");
        connectionManager.commit();
        
        OrderDto shippedOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("SHIPPED", shippedOrder.getStatus(), "Status must update to SHIPPED");
        assertNotNull(shippedOrder.getShippedDate(), "Shipped date must be set");
        
        // DELIVERED
        orderDao.updateStatus(orderId, "DELIVERED");
        connectionManager.commit();
        
        OrderDto deliveredOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("DELIVERED", deliveredOrder.getStatus(), "Status must update to DELIVERED");
        
        logger.info("Order status workflow completed: {} -> {} -> {} -> {}", 
                order.getStatus(), "CONFIRMED", "SHIPPED", "DELIVERED");

        logger.info("Order status workflow test passed");
    }
} 
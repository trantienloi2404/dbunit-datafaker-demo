package com.example.demo;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp test chức năng cho cơ sở dữ liệu thông qua các phương thức DAO.
 * Chứng minh việc test logic nghiệp vụ thông qua DAO như yêu cầu
 * cho việc test cơ sở dữ liệu toàn diện sử dụng mô hình DAO/DTO.
 * 
 * Functional test class for testing business logic through DAO methods.
 * This demonstrates functional testing of database business logic as required
 * for comprehensive database testing coverage using the DAO/DTO pattern.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseFunctionalTest {

    // Logger để ghi log thông tin test
    private static final Logger logger = LoggerFactory.getLogger(DatabaseFunctionalTest.class);
    
    // Quản lý kết nối cơ sở dữ liệu
    private DatabaseConnectionManager connectionManager;
    // Bộ tạo dữ liệu test sử dụng DataFaker
    private TestDataGenerator dataGenerator;
    // Tóm tắt dữ liệu test đã tạo
    private TestDataGenerator.TestDataSummary testDataSummary;
    
    // Các instance DAO để tương tác với cơ sở dữ liệu
    private UserDao userDao;
    private ProductDao productDao;
    private OrderDao orderDao;
    private OrderItemDao orderItemDao;
    private ReviewDao reviewDao;

    /**
     * Thiết lập môi trường test trước mỗi test case
     * Khởi tạo kết nối, DAO và tạo dữ liệu test
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Thiết lập test chức năng cơ sở dữ liệu");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();
        
        // Tạo bộ tạo dữ liệu và tạo dữ liệu test
        dataGenerator = new TestDataGenerator(12345L);
        testDataSummary = dataGenerator.generateCompleteTestData(5, 10, 3);
        connectionManager.commit();

        logger.info("Thiết lập test chức năng hoàn tất");
    }

    /**
     * Dọn dẹp sau mỗi test case
     * Xóa dữ liệu test và đóng kết nối
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Dọn dẹp dữ liệu test chức năng");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Dọn dẹp test chức năng hoàn tất");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    // ===============================
    // TEST CHỨC NĂNG
    // ===============================

    /**
     * Test hàm tính tổng đơn hàng trong cơ sở dữ liệu
     * Kiểm tra các hàm tính toán tổng tiền với thuế mặc định và tùy chỉnh
     */
    @Test
    @Order(1)
    @DisplayName("Test Hàm Tính Tổng Đơn Hàng")
    void testCalculateOrderTotalFunction() throws Exception {
        logger.info("Test hàm calculate_order_total");

        // Lấy một đơn hàng test
        Long orderId = testDataSummary.getOrderIds().get(0);
        
        // Test với thuế suất mặc định (8.75%)
        BigDecimal totalWithTax = orderDao.calculateOrderTotal(orderId);
        assertNotNull(totalWithTax, "Hàm phải trả về kết quả");
        assertTrue(totalWithTax.compareTo(BigDecimal.ZERO) > 0, 
                "Tổng tiền với thuế phải dương");
        
        logger.info("Đơn hàng {} tổng tiền với thuế mặc định: {}", orderId, totalWithTax);

        // Test với thuế suất tùy chỉnh (10%) sử dụng hàm riêng biệt
        BigDecimal customTaxRate = new BigDecimal("0.10"); // 10%
        BigDecimal totalWithCustomTax = orderDao.calculateOrderTotalWithTax(orderId, customTaxRate);
        assertNotNull(totalWithCustomTax, "Hàm phải trả về kết quả");
        assertTrue(totalWithCustomTax.compareTo(BigDecimal.ZERO) > 0, 
                "Tổng tiền với thuế tùy chỉnh phải dương");
        
        logger.info("Đơn hàng {} tổng tiền với thuế 10%: {}", orderId, totalWithCustomTax);

        logger.info("Test hàm calculate_order_total thành công");
    }

    /**
     * Test quản lý tồn kho sản phẩm
     * Kiểm tra logic giảm và tăng số lượng tồn kho
     */
    @Test
    @Order(2)
    @DisplayName("Test Quản Lý Tồn Kho Sản Phẩm")
    void testProductStockManagement() throws SQLException {
        logger.info("Test quản lý tồn kho sản phẩm");

        // Lấy một sản phẩm test
        Long productId = testDataSummary.getProductIds().get(0);
        ProductDto product = productDao.findById(productId).orElseThrow();
        int initialStock = product.getStockQuantity();
        
        logger.info("Tồn kho ban đầu cho sản phẩm {}: {}", productId, initialStock);

        // Test giảm tồn kho
        int reductionAmount = 5;
        productDao.reduceStock(productId, reductionAmount);
        connectionManager.commit();
        
        ProductDto updatedProduct = productDao.findById(productId).orElseThrow();
        assertEquals(initialStock - reductionAmount, updatedProduct.getStockQuantity(), 
                "Tồn kho phải được giảm đúng cách");
        
        logger.info("Tồn kho sau khi giảm: {}", updatedProduct.getStockQuantity());

        // Test tăng tồn kho
        int increaseAmount = 3;
        productDao.increaseStock(productId, increaseAmount);
        connectionManager.commit();
        
        ProductDto finalProduct = productDao.findById(productId).orElseThrow();
        assertEquals(initialStock - reductionAmount + increaseAmount, finalProduct.getStockQuantity(), 
                "Tồn kho phải được tăng đúng cách");
        
        logger.info("Tồn kho sau khi tăng: {}", finalProduct.getStockQuantity());

        logger.info("Test quản lý tồn kho sản phẩm thành công");
    }

    /**
     * Test quy trình trạng thái đơn hàng
     * Kiểm tra logic cập nhật trạng thái đơn hàng từ PENDING đến DELIVERED
     */
    @Test
    @Order(3)
    @DisplayName("Test Quy Trình Trạng Thái Đơn Hàng")
    void testOrderStatusWorkflow() throws SQLException {
        logger.info("Test quy trình trạng thái đơn hàng");

        // Lấy một đơn hàng test
        Long orderId = testDataSummary.getOrderIds().get(0);
        OrderDto order = orderDao.findById(orderId).orElseThrow();
        
        logger.info("Trạng thái ban đầu của đơn hàng {}: {}", orderId, order.getStatus());

        // Test cập nhật trạng thái theo quy trình nghiệp vụ
        // PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
        
        // Cập nhật thành CONFIRMED
        orderDao.updateStatus(orderId, "CONFIRMED");
        connectionManager.commit();
        
        OrderDto confirmedOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("CONFIRMED", confirmedOrder.getStatus(), "Trạng thái phải được cập nhật thành CONFIRMED");
        
        // Cập nhật thành SHIPPED
        orderDao.updateStatus(orderId, "SHIPPED");
        connectionManager.commit();
        
        OrderDto shippedOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("SHIPPED", shippedOrder.getStatus(), "Trạng thái phải được cập nhật thành SHIPPED");
        assertNotNull(shippedOrder.getShippedDate(), "Ngày gửi hàng phải được cập nhật");
        
        // Cập nhật thành DELIVERED
        orderDao.updateStatus(orderId, "DELIVERED");
        connectionManager.commit();
        
        OrderDto deliveredOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("DELIVERED", deliveredOrder.getStatus(), "Trạng thái phải được cập nhật thành DELIVERED");
        
        logger.info("Quy trình trạng thái đơn hàng hoàn tất: {} -> {} -> {} -> {}", 
                order.getStatus(), "CONFIRMED", "SHIPPED", "DELIVERED");

        logger.info("Test quy trình trạng thái đơn hàng thành công");
    }
} 
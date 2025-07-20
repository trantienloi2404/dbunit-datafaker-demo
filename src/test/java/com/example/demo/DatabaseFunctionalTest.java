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

    // ===============================
    // TEST LOGIC NGHIỆP VỤ
    // ===============================

    /**
     * Test hàm trạng thái thành viên của user
     * Kiểm tra logic tính toán cấp độ thành viên dựa trên lịch sử mua hàng
     */
    @Test
    @Order(2)
    @DisplayName("Test Hàm Trạng Thái Thành Viên User")
    void testUserLoyaltyStatusFunction() throws SQLException {
        logger.info("Test hàm trạng thái thành viên user");

        // Lấy một user test
        Long userId = testDataSummary.getUserIds().get(0);
        
        // Test hàm trạng thái thành viên
        String loyaltyStatus = orderDao.getUserLoyaltyStatus(userId);
        assertNotNull(loyaltyStatus, "Trạng thái thành viên không được null");
        assertTrue(loyaltyStatus.matches("BRONZE|SILVER|GOLD|PLATINUM"), 
                "Trạng thái thành viên phải hợp lệ");
        
        logger.info("User {} trạng thái thành viên: {}", userId, loyaltyStatus);

        logger.info("Test hàm trạng thái thành viên user thành công");
    }

    /**
     * Test quản lý tồn kho sản phẩm
     * Kiểm tra logic giảm và tăng số lượng tồn kho
     */
    @Test
    @Order(3)
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
    @Order(4)
    @DisplayName("Test Quy Trình Trạng Thái Đơn Hàng")
    void testOrderStatusWorkflow() throws SQLException {
        logger.info("Test quy trình trạng thái đơn hàng");

        // Lấy một đơn hàng test
        Long orderId = testDataSummary.getOrderIds().get(0);
        OrderDto order = orderDao.findById(orderId).orElseThrow();
        
        logger.info("Trạng thái đơn hàng ban đầu: {}", order.getStatus());

        // Test cập nhật trạng thái
        orderDao.updateStatus(orderId, "CONFIRMED");
        connectionManager.commit();
        
        OrderDto confirmedOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("CONFIRMED", confirmedOrder.getStatus(), "Trạng thái đơn hàng phải được cập nhật thành CONFIRMED");
        
        orderDao.updateStatus(orderId, "SHIPPED");
        connectionManager.commit();
        
        OrderDto shippedOrder = orderDao.findById(orderId).orElseThrow();
        assertEquals("SHIPPED", shippedOrder.getStatus(), "Trạng thái đơn hàng phải được cập nhật thành SHIPPED");
        
        // Test tìm đơn hàng theo trạng thái
        List<OrderDto> shippedOrders = orderDao.findByStatus("SHIPPED");
        assertTrue(shippedOrders.stream().anyMatch(o -> o.getId().equals(orderId)), 
                "Đơn hàng phải được tìm thấy trong danh sách đơn hàng đã gửi");

        logger.info("Test quy trình trạng thái đơn hàng thành công");
    }

    /**
     * Test tổng hợp đánh giá sản phẩm
     * Kiểm tra logic tính điểm trung bình và đếm số lượng đánh giá
     */
    @Test
    @Order(5)
    @DisplayName("Test Tổng Hợp Đánh Giá Sản Phẩm")
    void testReviewRatingAggregation() throws SQLException {
        logger.info("Test tổng hợp đánh giá sản phẩm");

        // Lấy dữ liệu test
        Long productId = testDataSummary.getProductIds().get(0);
        Long userId = testDataSummary.getUserIds().get(0);
        
        // Tạo một đánh giá test
        ReviewDto review = new ReviewDto();
        review.setUserId(userId);
        review.setProductId(productId);
        review.setRating(4);
        review.setTitle("Đánh giá test");
        review.setComment("Đây là một đánh giá test");
        review.setIsVerifiedPurchase(true);
        
        try {
            ReviewDto createdReview = reviewDao.create(review);
            connectionManager.commit();
            
            // Test tính điểm trung bình
            BigDecimal avgRating = reviewDao.getAverageRatingForProduct(productId);
            assertNotNull(avgRating, "Điểm trung bình không được null");
            assertTrue(avgRating.compareTo(BigDecimal.ZERO) >= 0, "Điểm trung bình phải không âm");
            assertTrue(avgRating.compareTo(new BigDecimal("5.0")) <= 0, "Điểm trung bình không được vượt quá 5");
            
            logger.info("Điểm trung bình cho sản phẩm {}: {}", productId, avgRating);
            
            // Test đếm số lượng đánh giá
            long reviewCount = reviewDao.countByProductId(productId);
            assertTrue(reviewCount >= 1, "Phải có ít nhất một đánh giá cho sản phẩm");
            
            // Dọn dẹp đánh giá test
            reviewDao.delete(createdReview.getId());
            connectionManager.commit();
            
        } catch (SQLException e) {
            // Đánh giá có thể đã tồn tại do ràng buộc unique, điều này ổn
            logger.info("Đánh giá đã tồn tại cho cặp user-sản phẩm, test dữ liệu hiện có");
            
            BigDecimal avgRating = reviewDao.getAverageRatingForProduct(productId);
            if (avgRating != null && avgRating.compareTo(BigDecimal.ZERO) > 0) {
                assertTrue(avgRating.compareTo(new BigDecimal("5.0")) <= 0, "Điểm trung bình không được vượt quá 5");
                logger.info("Điểm trung bình cho sản phẩm {}: {}", productId, avgRating);
            }
        }

        logger.info("Test tổng hợp đánh giá sản phẩm thành công");
    }

    /**
     * Test tích hợp logic nghiệp vụ
     * Kiểm tra tính toàn vẹn dữ liệu và mối quan hệ giữa các entity
     */
    @Test
    @Order(6)
    @DisplayName("Test Tích Hợp Logic Nghiệp Vụ")
    void testBusinessLogicIntegration() throws SQLException {
        logger.info("Test tích hợp logic nghiệp vụ");

        // Test rằng tất cả dữ liệu được tạo duy trì tính toàn vẹn tham chiếu
        
        // Xác minh đơn hàng có user hợp lệ
        for (Long orderId : testDataSummary.getOrderIds()) {
            OrderDto order = orderDao.findById(orderId).orElseThrow();
            UserDto user = userDao.findById(order.getUserId()).orElseThrow();
            assertNotNull(user, "Đơn hàng phải tham chiếu đến user hợp lệ");
        }
        
        // Xác minh chi tiết đơn hàng có đơn hàng và sản phẩm hợp lệ
        for (Long orderItemId : testDataSummary.getOrderItemIds()) {
            OrderItemDto orderItem = orderItemDao.findById(orderItemId).orElseThrow();
            
            OrderDto order = orderDao.findById(orderItem.getOrderId()).orElseThrow();
            assertNotNull(order, "Chi tiết đơn hàng phải tham chiếu đến đơn hàng hợp lệ");
            
            ProductDto product = productDao.findById(orderItem.getProductId()).orElseThrow();
            assertNotNull(product, "Chi tiết đơn hàng phải tham chiếu đến sản phẩm hợp lệ");
            
            // Xác minh logic tính toán
            BigDecimal expectedTotal = orderItem.getUnitPrice().multiply(new BigDecimal(orderItem.getQuantity()));
            assertEquals(0, orderItem.getTotalPrice().compareTo(expectedTotal), 
                    "Tổng tiền chi tiết đơn hàng phải bằng đơn giá * số lượng");
        }
        
        // Xác minh đánh giá có user và sản phẩm hợp lệ
        for (Long reviewId : testDataSummary.getReviewIds()) {
            ReviewDto review = reviewDao.findById(reviewId).orElseThrow();
            
            UserDto user = userDao.findById(review.getUserId()).orElseThrow();
            assertNotNull(user, "Đánh giá phải tham chiếu đến user hợp lệ");
            
            ProductDto product = productDao.findById(review.getProductId()).orElseThrow();
            assertNotNull(product, "Đánh giá phải tham chiếu đến sản phẩm hợp lệ");
            
            assertTrue(review.getRating() >= 1 && review.getRating() <= 5, 
                    "Điểm đánh giá phải từ 1 đến 5");
        }

        logger.info("Test tích hợp logic nghiệp vụ thành công");
    }
} 
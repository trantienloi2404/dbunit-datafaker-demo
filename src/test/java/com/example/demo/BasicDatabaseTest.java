package com.example.demo;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp test cơ bản cho cơ sở dữ liệu sử dụng DAO thay vì truy cập trực tiếp.
 * Chứng minh chức năng DAO cơ bản cho việc test cơ sở dữ liệu theo nguyên tắc
 * rằng các test chỉ nên tương tác với các phương thức DAO, không phải SQL thô.
 * 
 * Basic test class demonstrating DAO operations instead of direct database access.
 * Shows fundamental DAO functionality for database testing following the principle
 * that tests should only interact with DAO methods, not raw SQL.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BasicDatabaseTest {

    // Logger để ghi log thông tin test
    private static final Logger logger = LoggerFactory.getLogger(BasicDatabaseTest.class);

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
        logger.info("Thiết lập test cơ sở dữ liệu cơ bản");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();

        // Tạo bộ tạo dữ liệu test với seed cố định để có thể tái tạo
        dataGenerator = new TestDataGenerator(12345L);

        // Tạo dữ liệu test sử dụng lớp DAO
        testDataSummary = dataGenerator.generateCompleteTestData(3, 5, 2);
        connectionManager.commit();

        logger.info("Thiết lập test cơ bản hoàn tất");
    }

    /**
     * Dọn dẹp sau mỗi test case
     * Xóa dữ liệu test và đóng kết nối
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Dọn dẹp dữ liệu test cơ bản");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Dọn dẹp test cơ bản hoàn tất");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    /**
     * Test kết nối cơ sở dữ liệu thông qua lớp DAO
     * Kiểm tra xem có thể thực hiện các thao tác DAO cơ bản không
     */
    @Test
    @Order(1)
    @DisplayName("Test Kết Nối Cơ Sở Dữ Liệu Qua Lớp DAO")
    void testDatabaseConnectionThroughDao() throws SQLException {
        logger.info("Test kết nối cơ sở dữ liệu qua lớp DAO");
        
        // Test kết nối bằng cách thực hiện các thao tác DAO đơn giản
        long userCount = userDao.count();
        long productCount = productDao.count();
        long orderCount = orderDao.count();
        
        assertTrue(userCount >= 0, "Số lượng user phải không âm");
        assertTrue(productCount >= 0, "Số lượng sản phẩm phải không âm");
        assertTrue(orderCount >= 0, "Số lượng đơn hàng phải không âm");
        
        logger.info("Test kết nối cơ sở dữ liệu thành công - Users: {}, Products: {}, Orders: {}", 
                userCount, productCount, orderCount);
    }

    /**
     * Test tạo dữ liệu thông qua các phương thức DAO
     * Xác minh rằng dữ liệu test được tạo đúng cách
     */
    @Test
    @Order(2)
    @DisplayName("Test Tạo Dữ Liệu Qua Các Phương Thức DAO")
    void testDataCreationThroughDao() throws SQLException {
        logger.info("Test tạo dữ liệu qua các phương thức DAO");
        
        // Xác minh rằng dữ liệu test được tạo đúng cách
        assertEquals(3, testDataSummary.getUserCount(), "Phải có 3 user trong dữ liệu test");
        assertEquals(5, testDataSummary.getProductCount(), "Phải có 5 sản phẩm trong dữ liệu test");
        assertEquals(2, testDataSummary.getOrderCount(), "Phải có 2 đơn hàng trong dữ liệu test");
        
        // Xác minh dữ liệu tồn tại trong cơ sở dữ liệu qua các phương thức DAO
        List<UserDto> users = userDao.findAll();
        List<ProductDto> products = productDao.findAll();
        List<OrderDto> orders = orderDao.findAll();
        
        assertTrue(users.size() >= 3, "Phải có ít nhất 3 user");
        assertTrue(products.size() >= 5, "Phải có ít nhất 5 sản phẩm");
        assertTrue(orders.size() >= 2, "Phải có ít nhất 2 đơn hàng");

        logger.info("Test tạo dữ liệu thành công");
    }

    /**
     * Test các thao tác DAO cho User
     * Kiểm tra CRUD operations và các query đặc biệt cho user
     */
    @Test
    @Order(3)
    @DisplayName("Test Các Thao Tác DAO Cho User")
    void testUserDaoOperations() throws SQLException {
        logger.info("Test các thao tác DAO cho User");
        
        // Lấy một user test
        Long userId = testDataSummary.getUserIds().get(0);
        UserDto user = userDao.findById(userId).orElseThrow();
        
        // Xác minh tính toàn vẹn dữ liệu user
        assertNotNull(user.getUsername(), "Username không được null");
        assertNotNull(user.getEmail(), "Email không được null");
        assertNotNull(user.getFirstName(), "Tên không được null");
        assertNotNull(user.getLastName(), "Họ không được null");
        assertTrue(user.getIsActive(), "User phải active theo mặc định");
        
        // Test tìm user theo username
        UserDto foundByUsername = userDao.findByUsername(user.getUsername()).orElseThrow();
        assertEquals(user.getId(), foundByUsername.getId(), "Phải tìm thấy cùng user theo username");
        
        // Test tìm user theo email
        UserDto foundByEmail = userDao.findByEmail(user.getEmail()).orElseThrow();
        assertEquals(user.getId(), foundByEmail.getId(), "Phải tìm thấy cùng user theo email");
        
        // Test query user active
        List<UserDto> activeUsers = userDao.findAllActive();
        assertTrue(activeUsers.size() >= 3, "Phải có ít nhất 3 user active");
        
        logger.info("Test thao tác DAO User thành công cho user: {}", user.getUsername());
    }

    /**
     * Test các thao tác DAO cho Product
     * Kiểm tra CRUD operations và các query đặc biệt cho sản phẩm
     */
    @Test
    @Order(4)
    @DisplayName("Test Các Thao Tác DAO Cho Product")
    void testProductDaoOperations() throws SQLException {
        logger.info("Test các thao tác DAO cho Product");
        
        // Lấy một sản phẩm test
        Long productId = testDataSummary.getProductIds().get(0);
        ProductDto product = productDao.findById(productId).orElseThrow();
        
        // Xác minh tính toàn vẹn dữ liệu sản phẩm
        assertNotNull(product.getName(), "Tên sản phẩm không được null");
        assertNotNull(product.getSku(), "SKU sản phẩm không được null");
        assertNotNull(product.getCategory(), "Danh mục sản phẩm không được null");
        assertNotNull(product.getPrice(), "Giá sản phẩm không được null");
        assertTrue(product.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0, "Giá phải dương");
        assertTrue(product.getStockQuantity() >= 0, "Số lượng tồn kho phải không âm");
        
        // Test tìm sản phẩm theo SKU
        ProductDto foundBySku = productDao.findBySku(product.getSku()).orElseThrow();
        assertEquals(product.getId(), foundBySku.getId(), "Phải tìm thấy cùng sản phẩm theo SKU");
        
        // Test tìm sản phẩm theo danh mục
        List<ProductDto> categoryProducts = productDao.findByCategory(product.getCategory());
        assertTrue(categoryProducts.size() >= 1, "Phải có ít nhất 1 sản phẩm trong danh mục");
        assertTrue(categoryProducts.stream().anyMatch(p -> p.getId().equals(product.getId())), 
                "Phải bao gồm sản phẩm test của chúng ta");
        
        logger.info("Test thao tác DAO Product thành công cho sản phẩm: {}", product.getName());
    }

    /**
     * Test các thao tác DAO cho Order và OrderItem
     * Kiểm tra CRUD operations và mối quan hệ giữa đơn hàng và chi tiết đơn hàng
     */
    @Test
    @Order(5)
    @DisplayName("Test Các Thao Tác DAO Cho Order Và OrderItem")
    void testOrderAndOrderItemDaoOperations() throws SQLException {
        logger.info("Test các thao tác DAO cho Order và OrderItem");
        
        // Lấy một đơn hàng test
        Long orderId = testDataSummary.getOrderIds().get(0);
        OrderDto order = orderDao.findById(orderId).orElseThrow();
        
        // Xác minh tính toàn vẹn dữ liệu đơn hàng
        assertNotNull(order.getUserId(), "User ID của đơn hàng không được null");
        assertNotNull(order.getOrderNumber(), "Số đơn hàng không được null");
        assertNotNull(order.getTotalAmount(), "Tổng tiền đơn hàng không được null");
        assertNotNull(order.getStatus(), "Trạng thái đơn hàng không được null");
        assertTrue(order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0, "Tổng tiền phải dương");
        
        // Test tìm đơn hàng theo số đơn hàng
        OrderDto foundByOrderNumber = orderDao.findByOrderNumber(order.getOrderNumber()).orElseThrow();
        assertEquals(order.getId(), foundByOrderNumber.getId(), "Phải tìm thấy cùng đơn hàng theo số đơn hàng");
        
        // Test tìm đơn hàng theo user
        List<OrderDto> userOrders = orderDao.findByUserId(order.getUserId());
        assertTrue(userOrders.size() >= 1, "Phải có ít nhất 1 đơn hàng cho user");
        assertTrue(userOrders.stream().anyMatch(o -> o.getId().equals(order.getId())), 
                "Phải bao gồm đơn hàng test của chúng ta");
        
        // Test chi tiết đơn hàng
        List<OrderItemDto> orderItems = orderItemDao.findByOrderId(orderId);
        for (OrderItemDto item : orderItems) {
            assertEquals(orderId, item.getOrderId(), "Chi tiết đơn hàng phải thuộc về đơn hàng đúng");
            assertTrue(item.getQuantity() > 0, "Số lượng phải dương");
            assertTrue(item.getUnitPrice().compareTo(java.math.BigDecimal.ZERO) > 0, "Đơn giá phải dương");
            assertTrue(item.getTotalPrice().compareTo(java.math.BigDecimal.ZERO) > 0, "Tổng tiền phải dương");
        }
        
        logger.info("Test thao tác DAO Order thành công cho đơn hàng: {}", order.getOrderNumber());
    }

    /**
     * Test các thao tác DAO cho Review
     * Kiểm tra CRUD operations và các query đặc biệt cho đánh giá
     */
    @Test
    @Order(6)
    @DisplayName("Test Các Thao Tác DAO Cho Review")
    void testReviewDaoOperations() throws SQLException {
        logger.info("Test các thao tác DAO cho Review");
        
        // Kiểm tra xem có đánh giá nào không
        List<ReviewDto> allReviews = reviewDao.findAll();
        
        if (!allReviews.isEmpty()) {
            ReviewDto review = allReviews.get(0);
            
            // Xác minh tính toàn vẹn dữ liệu đánh giá
            assertNotNull(review.getUserId(), "User ID của đánh giá không được null");
            assertNotNull(review.getProductId(), "Product ID của đánh giá không được null");
            assertNotNull(review.getRating(), "Điểm đánh giá không được null");
            assertTrue(review.getRating() >= 1 && review.getRating() <= 5, "Điểm phải từ 1 đến 5");
            
            // Test tìm đánh giá theo user
            List<ReviewDto> userReviews = reviewDao.findByUserId(review.getUserId());
            assertTrue(userReviews.size() >= 1, "Phải có ít nhất 1 đánh giá cho user");
            
            // Test tìm đánh giá theo sản phẩm
            List<ReviewDto> productReviews = reviewDao.findByProductId(review.getProductId());
            assertTrue(productReviews.size() >= 1, "Phải có ít nhất 1 đánh giá cho sản phẩm");
            
            // Test tính điểm trung bình
            java.math.BigDecimal avgRating = reviewDao.getAverageRatingForProduct(review.getProductId());
            assertTrue(avgRating.compareTo(java.math.BigDecimal.ZERO) >= 0, "Điểm trung bình phải không âm");
            assertTrue(avgRating.compareTo(new java.math.BigDecimal("5.0")) <= 0, "Điểm trung bình không được vượt quá 5");
            
            logger.info("Test thao tác DAO Review thành công");
        } else {
            logger.info("Không tìm thấy đánh giá trong dữ liệu test - bỏ qua các validation đặc biệt cho review");
        }
    }

    /**
     * Test số lượng dữ liệu và tính nhất quán
     * Kiểm tra các phương thức đếm và tính nhất quán của dữ liệu
     */
    @Test
    @Order(7)
    @DisplayName("Test Số Lượng Dữ Liệu Và Tính Nhất Quán")
    void testDataCountsAndConsistency() throws SQLException {
        logger.info("Test số lượng dữ liệu và tính nhất quán");
        
        // Test các phương thức đếm
        long userCount = userDao.count();
        long activeUserCount = userDao.countActive();
        long productCount = productDao.count();
        long orderCount = orderDao.count();
        long orderItemCount = orderItemDao.count();
        long reviewCount = reviewDao.count();
        
        // Xác minh số lượng có ý nghĩa
        assertTrue(activeUserCount <= userCount, "Số user active không được vượt quá tổng số user");
        assertTrue(orderItemCount >= orderCount, "Phải có ít nhất nhiều chi tiết đơn hàng như số đơn hàng");
        
        // Test đếm theo trạng thái
        if (orderCount > 0) {
            long pendingOrders = orderDao.countByStatus("PENDING");
            long deliveredOrders = orderDao.countByStatus("DELIVERED");
            long shippedOrders = orderDao.countByStatus("SHIPPED");
            
            assertTrue(pendingOrders + deliveredOrders + shippedOrders <= orderCount, 
                    "Tổng số đếm theo trạng thái không được vượt quá tổng số đơn hàng");
        }
        
        logger.info("Số lượng dữ liệu - Users: {} (Active: {}), Products: {}, Orders: {}, OrderItems: {}, Reviews: {}", 
                userCount, activeUserCount, productCount, orderCount, orderItemCount, reviewCount);
        
        logger.info("Test số lượng dữ liệu và tính nhất quán thành công");
    }
}
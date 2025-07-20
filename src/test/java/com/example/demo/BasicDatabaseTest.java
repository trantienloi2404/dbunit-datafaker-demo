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
     * Test các thao tác DAO cơ bản cho tất cả entities
     * Kiểm tra CRUD operations và các query đặc biệt cho tất cả entities
     */
    @Test
    @Order(3)
    @DisplayName("Test Các Thao Tác DAO Cơ Bản")
    void testBasicDaoOperations() throws SQLException {
        logger.info("Test các thao tác DAO cơ bản cho tất cả entities");
        
        // Test User DAO operations
        Long userId = testDataSummary.getUserIds().get(0);
        UserDto user = userDao.findById(userId).orElseThrow();
        assertNotNull(user.getUsername(), "Username không được null");
        assertTrue(user.getIsActive(), "User phải active theo mặc định");
        
        UserDto foundByUsername = userDao.findByUsername(user.getUsername()).orElseThrow();
        assertEquals(user.getId(), foundByUsername.getId(), "Phải tìm thấy cùng user theo username");
        
        // Test Product DAO operations
        Long productId = testDataSummary.getProductIds().get(0);
        ProductDto product = productDao.findById(productId).orElseThrow();
        assertNotNull(product.getName(), "Tên sản phẩm không được null");
        assertTrue(product.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0, "Giá phải dương");
        
        ProductDto foundBySku = productDao.findBySku(product.getSku()).orElseThrow();
        assertEquals(product.getId(), foundBySku.getId(), "Phải tìm thấy cùng sản phẩm theo SKU");
        
        // Test Order DAO operations
        Long orderId = testDataSummary.getOrderIds().get(0);
        OrderDto order = orderDao.findById(orderId).orElseThrow();
        assertNotNull(order.getOrderNumber(), "Số đơn hàng không được null");
        assertTrue(order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0, "Tổng tiền phải dương");
        
        // Test OrderItem relationships
        List<OrderItemDto> orderItems = orderItemDao.findByOrderId(orderId);
        assertFalse(orderItems.isEmpty(), "Đơn hàng phải có ít nhất 1 chi tiết");
        
        logger.info("Test các thao tác DAO cơ bản thành công");
    }

    /**
     * Test số lượng dữ liệu và tính nhất quán
     * Kiểm tra các phương thức đếm và tính nhất quán của dữ liệu
     */
    @Test
    @Order(4)
    @DisplayName("Test Số Lượng Dữ Liệu Và Tính Nhất Quán")
    void testDataCountsAndConsistency() throws SQLException {
        logger.info("Test số lượng dữ liệu và tính nhất quán");
        
        // Test các phương thức đếm
        long userCount = userDao.count();
        long activeUserCount = userDao.countActive();
        long productCount = productDao.count();
        long orderCount = orderDao.count();
        long orderItemCount = orderItemDao.count();
        
        // Xác minh số lượng có ý nghĩa
        assertTrue(activeUserCount <= userCount, "Số user active không được vượt quá tổng số user");
        assertTrue(orderItemCount >= orderCount, "Phải có ít nhất nhiều chi tiết đơn hàng như số đơn hàng");
        
        logger.info("Số lượng dữ liệu - Users: {} (Active: {}), Products: {}, Orders: {}, OrderItems: {}", 
                userCount, activeUserCount, productCount, orderCount, orderItemCount);
        
        logger.info("Test số lượng dữ liệu và tính nhất quán thành công");
    }
}
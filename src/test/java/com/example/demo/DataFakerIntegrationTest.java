package com.example.demo;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp test tích hợp cho chức năng DataFaker với lớp DAO.
 * Test việc tạo dữ liệu thực tế và tích hợp cơ sở dữ liệu thông qua các phương
 * thức DAO.
 * 
 * Integration test class for DataFaker functionality with DAO layer.
 * Tests realistic data generation and database integration through DAO methods.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataFakerIntegrationTest {

    // Logger để ghi log thông tin test
    private static final Logger logger = LoggerFactory.getLogger(DataFakerIntegrationTest.class);

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
     * Khởi tạo kết nối và DAO
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Thiết lập test tích hợp DataFaker");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();

        // Tạo bộ tạo dữ liệu với seed cố định để test có thể tái tạo
        dataGenerator = new TestDataGenerator(12345L);

        logger.info("Thiết lập test tích hợp DataFaker hoàn tất");
    }

    /**
     * Dọn dẹp sau mỗi test case
     * Xóa dữ liệu test và đóng kết nối
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Dọn dẹp dữ liệu test DataFaker");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Dọn dẹp test DataFaker hoàn tất");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    /**
     * Test tạo bộ dữ liệu nhỏ
     * Kiểm tra việc tạo dữ liệu với số lượng nhỏ và xác minh kết quả
     */
    @Test
    @Order(1)
    @DisplayName("Test Tạo Bộ Dữ Liệu Nhỏ")
    void testSmallDatasetGeneration() throws Exception {
        logger.info("Test tạo bộ dữ liệu nhỏ");

        // Tạo bộ dữ liệu nhỏ
        testDataSummary = dataGenerator.generateCompleteTestData(5, 8, 3);
        connectionManager.commit();

        // Xác minh tóm tắt dữ liệu được tạo
        assertEquals(5, testDataSummary.getUserCount(), "Phải có 5 user");
        assertEquals(8, testDataSummary.getProductCount(), "Phải có 8 sản phẩm");
        assertEquals(3, testDataSummary.getOrderCount(), "Phải có 3 đơn hàng");

        // Xác minh dữ liệu tồn tại trong cơ sở dữ liệu qua các phương thức DAO
        long userCount = userDao.count();
        long productCount = productDao.count();
        long orderCount = orderDao.count();

        assertTrue(userCount >= 5, "Cơ sở dữ liệu phải chứa ít nhất 5 user");
        assertTrue(productCount >= 8, "Cơ sở dữ liệu phải chứa ít nhất 8 sản phẩm");
        assertTrue(orderCount >= 3, "Cơ sở dữ liệu phải chứa ít nhất 3 đơn hàng");

        logger.info("Test tạo bộ dữ liệu nhỏ thành công");
    }

    /**
     * Test tính duy nhất và tính toàn vẹn của dữ liệu
     * Kiểm tra rằng dữ liệu được tạo không có trùng lặp và đảm bảo tính toàn vẹn
     */
    @Test
    @Order(2)
    @DisplayName("Test Tính Duy Nhất Và Tính Toàn Vẹn Dữ Liệu")
    void testDataUniquenessAndIntegrity() throws Exception {
        logger.info("Test tính duy nhất và tính toàn vẹn dữ liệu");

        // Tạo bộ dữ liệu với khả năng trùng lặp
        testDataSummary = dataGenerator.generateCompleteTestData(10, 15, 8);
        connectionManager.commit();

        // Test tính duy nhất username
        List<UserDto> allUsers = userDao.findAll();
        Set<String> usernames = new HashSet<>();
        for (UserDto user : allUsers) {
            assertFalse(usernames.contains(user.getUsername()),
                    "Tất cả username phải duy nhất: " + user.getUsername());
            usernames.add(user.getUsername());
        }

        // Test tính duy nhất email
        Set<String> emails = new HashSet<>();
        for (UserDto user : allUsers) {
            assertFalse(emails.contains(user.getEmail()),
                    "Tất cả email phải duy nhất: " + user.getEmail());
            emails.add(user.getEmail());
        }

        // Test tính duy nhất SKU sản phẩm
        List<ProductDto> allProducts = productDao.findAll();
        Set<String> skus = new HashSet<>();
        for (ProductDto product : allProducts) {
            assertFalse(skus.contains(product.getSku()),
                    "Tất cả SKU sản phẩm phải duy nhất: " + product.getSku());
            skus.add(product.getSku());
        }

        // Test tính toàn vẹn khóa ngoại - đơn hàng phải có tham chiếu user hợp lệ
        List<OrderDto> allOrders = orderDao.findAll();
        for (OrderDto order : allOrders) {
            UserDto user = userDao.findById(order.getUserId()).orElse(null);
            assertNotNull(user, "Tất cả đơn hàng phải có tham chiếu user hợp lệ");
        }

        logger.info("Test tính duy nhất và tính toàn vẹn dữ liệu thành công");
    }

    /**
     * Test hiệu suất với bộ dữ liệu lớn
     * Kiểm tra thời gian tạo dữ liệu lớn và đảm bảo hiệu suất chấp nhận được
     */
    @Test
    @Order(3)
    @DisplayName("Test Hiệu Suất Bộ Dữ Liệu Lớn")
    void testLargeDatasetPerformance() throws Exception {
        logger.info("Test hiệu suất bộ dữ liệu lớn");

        long startTime = System.currentTimeMillis();

        // Tạo bộ dữ liệu lớn hơn
        testDataSummary = dataGenerator.generateCompleteTestData(50, 100, 30);
        connectionManager.commit();

        long generationTime = System.currentTimeMillis() - startTime;

        // Xác minh dữ liệu được tạo
        assertEquals(50, testDataSummary.getUserCount(), "Phải có 50 user");
        assertEquals(100, testDataSummary.getProductCount(), "Phải có 100 sản phẩm");
        assertEquals(30, testDataSummary.getOrderCount(), "Phải có 30 đơn hàng");

        // Kiểm tra hiệu suất (điều chỉnh ngưỡng khi cần)
        assertTrue(generationTime < 30000, "Việc tạo bộ dữ liệu lớn phải hoàn thành trong vòng 30 giây");

        logger.info("Bộ dữ liệu lớn được tạo trong {} ms", generationTime);
        logger.info("Test hiệu suất bộ dữ liệu lớn thành công");
    }

    /**
     * Test chất lượng và tính thực tế của dữ liệu
     * Kiểm tra rằng dữ liệu được tạo có chất lượng tốt và thực tế
     */
    @Test
    @Order(4)
    @DisplayName("Test Chất Lượng Và Tính Thực Tế Dữ Liệu")
    void testDataQualityAndRealism() throws Exception {
        logger.info("Test chất lượng và tính thực tế dữ liệu");

        // Tạo bộ dữ liệu test
        testDataSummary = dataGenerator.generateCompleteTestData(20, 30, 15);
        connectionManager.commit();

        // Test chất lượng dữ liệu user
        List<UserDto> users = userDao.findAll();
        for (UserDto user : users) {
            assertNotNull(user.getUsername(), "Username không được null");
            assertNotNull(user.getEmail(), "Email không được null");
            assertNotNull(user.getFirstName(), "Tên không được null");
            assertNotNull(user.getLastName(), "Họ không được null");
            assertTrue(user.getEmail().contains("@"), "Email phải có định dạng hợp lệ");
            assertTrue(user.getUsername().length() > 0, "Username không được rỗng");
        }

        // Test chất lượng dữ liệu sản phẩm
        List<ProductDto> products = productDao.findAll();
        for (ProductDto product : products) {
            assertNotNull(product.getName(), "Tên sản phẩm không được null");
            assertNotNull(product.getSku(), "SKU sản phẩm không được null");
            assertNotNull(product.getCategory(), "Danh mục sản phẩm không được null");
            assertNotNull(product.getPrice(), "Giá sản phẩm không được null");
            assertTrue(product.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0,
                    "Giá sản phẩm phải dương");
            assertTrue(product.getStockQuantity() >= 0, "Số lượng tồn kho phải không âm");
        }

        // Test chất lượng dữ liệu đơn hàng
        List<OrderDto> orders = orderDao.findAll();
        for (OrderDto order : orders) {
            assertNotNull(order.getOrderNumber(), "Số đơn hàng không được null");
            assertNotNull(order.getTotalAmount(), "Tổng tiền không được null");
            assertNotNull(order.getStatus(), "Trạng thái đơn hàng không được null");
            assertTrue(order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0,
                    "Tổng tiền đơn hàng phải dương");
            assertTrue(order.getOrderNumber().length() > 0, "Số đơn hàng không được rỗng");
        }

        logger.info("Test chất lượng và tính thực tế dữ liệu thành công");
    }

    /**
     * Test mối quan hệ dữ liệu
     * Kiểm tra các mối quan hệ giữa các entity và tính nhất quán
     */
    @Test
    @Order(5)
    @DisplayName("Test Mối Quan Hệ Dữ Liệu")
    void testDataRelationships() throws Exception {
        logger.info("Test mối quan hệ dữ liệu");

        // Tạo bộ dữ liệu test
        testDataSummary = dataGenerator.generateCompleteTestData(10, 20, 5);
        connectionManager.commit();

        // Test mối quan hệ đơn hàng-chi tiết đơn hàng
        List<OrderDto> orders = orderDao.findAll();
        for (OrderDto order : orders) {
            List<OrderItemDto> orderItems = orderItemDao.findByOrderId(order.getId());

            for (OrderItemDto item : orderItems) {
                assertEquals(order.getId(), item.getOrderId(),
                        "Chi tiết đơn hàng phải tham chiếu đến đơn hàng đúng");

                // Xác minh sản phẩm tồn tại
                ProductDto product = productDao.findById(item.getProductId()).orElse(null);
                assertNotNull(product, "Chi tiết đơn hàng phải tham chiếu đến sản phẩm hợp lệ");

                // Xác minh tính toán
                java.math.BigDecimal expectedTotal = item.getUnitPrice().multiply(
                        new java.math.BigDecimal(item.getQuantity()));
                assertEquals(0, item.getTotalPrice().compareTo(expectedTotal),
                        "Tổng tiền chi tiết đơn hàng phải bằng đơn giá * số lượng");
            }
        }

        // Test mối quan hệ user-đơn hàng
        for (OrderDto order : orders) {
            UserDto user = userDao.findById(order.getUserId()).orElse(null);
            assertNotNull(user, "Đơn hàng phải tham chiếu đến user hợp lệ");
        }

        // Test mối quan hệ user-đánh giá và sản phẩm-đánh giá
        List<ReviewDto> reviews = reviewDao.findAll();
        for (ReviewDto review : reviews) {
            UserDto user = userDao.findById(review.getUserId()).orElse(null);
            assertNotNull(user, "Đánh giá phải tham chiếu đến user hợp lệ");

            ProductDto product = productDao.findById(review.getProductId()).orElse(null);
            assertNotNull(product, "Đánh giá phải tham chiếu đến sản phẩm hợp lệ");

            assertTrue(review.getRating() >= 1 && review.getRating() <= 5,
                    "Điểm đánh giá phải từ 1 đến 5");
        }

        logger.info("Test mối quan hệ dữ liệu thành công");
    }

    /**
     * Test chức năng dọn dẹp
     * Kiểm tra rằng dữ liệu test có thể được dọn dẹp hoàn toàn
     */
    @Test
    @Order(6)
    @DisplayName("Test Chức Năng Dọn Dẹp")
    void testCleanupFunctionality() throws Exception {
        logger.info("Test chức năng dọn dẹp");

        // Tạo bộ dữ liệu test
        testDataSummary = dataGenerator.generateCompleteTestData(5, 10, 3);
        connectionManager.commit();

        // Xác minh dữ liệu tồn tại
        assertTrue(userDao.count() >= 5, "Phải có user được tạo");
        assertTrue(productDao.count() >= 10, "Phải có sản phẩm được tạo");
        assertTrue(orderDao.count() >= 3, "Phải có đơn hàng được tạo");

        // Thực hiện dọn dẹp
        dataGenerator.cleanupTestData(testDataSummary);
        connectionManager.commit();

        // Xác minh dữ liệu test cụ thể đã được dọn dẹp
        for (Long userId : testDataSummary.getUserIds()) {
            assertFalse(userDao.findById(userId).isPresent(),
                    "User test phải được dọn dẹp");
        }

        for (Long productId : testDataSummary.getProductIds()) {
            assertFalse(productDao.findById(productId).isPresent(),
                    "Sản phẩm test phải được dọn dẹp");
        }

        for (Long orderId : testDataSummary.getOrderIds()) {
            assertFalse(orderDao.findById(orderId).isPresent(),
                    "Đơn hàng test phải được dọn dẹp");
        }

        // Xóa tóm tắt để tránh dọn dẹp kép trong tearDown
        testDataSummary = null;

        logger.info("Test chức năng dọn dẹp thành công");
    }
}
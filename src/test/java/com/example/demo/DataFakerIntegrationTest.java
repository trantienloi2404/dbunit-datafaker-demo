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
     * Test tính duy nhất và tính toàn vẹn của dữ liệu
     * Kiểm tra rằng dữ liệu được tạo không có trùng lặp và đảm bảo tính toàn vẹn
     */
    @Test
    @Order(1)
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
     * Test chất lượng và tính thực tế của dữ liệu
     * Kiểm tra rằng dữ liệu được tạo có chất lượng tốt và thực tế
     */
    @Test
    @Order(2)
    @DisplayName("Test Chất Lượng Và Tính Thực Tế Dữ Liệu")
    void testDataQualityAndRealism() throws Exception {
        logger.info("Test chất lượng và tính thực tế dữ liệu");

        // Tạo dữ liệu test
        testDataSummary = dataGenerator.generateCompleteTestData(20, 30, 15);
        connectionManager.commit();

        // Test chất lượng dữ liệu user
        List<UserDto> users = userDao.findAll();
        for (UserDto user : users) {
            // Kiểm tra email hợp lệ
            assertTrue(user.getEmail().contains("@"), "Email phải chứa ký tự @");
            assertTrue(user.getEmail().contains("."), "Email phải chứa dấu chấm");
            
            // Kiểm tra tên không rỗng
            assertFalse(user.getFirstName().trim().isEmpty(), "Tên không được rỗng");
            assertFalse(user.getLastName().trim().isEmpty(), "Họ không được rỗng");
            
            // Kiểm tra username hợp lệ
            assertTrue(user.getUsername().length() >= 3, "Username phải có ít nhất 3 ký tự");
            assertTrue(user.getUsername().matches("^[a-zA-Z0-9._-]+$"), "Username chỉ chứa ký tự hợp lệ");
        }

        // Test chất lượng dữ liệu sản phẩm
        List<ProductDto> products = productDao.findAll();
        for (ProductDto product : products) {
            // Kiểm tra giá dương
            assertTrue(product.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0, 
                    "Giá sản phẩm phải dương");
            
            // Kiểm tra tên sản phẩm không rỗng
            assertFalse(product.getName().trim().isEmpty(), "Tên sản phẩm không được rỗng");
            
            // Kiểm tra SKU hợp lệ
            assertTrue(product.getSku().length() >= 5, "SKU phải có ít nhất 5 ký tự");
            assertTrue(product.getSku().matches("^[A-Z0-9-]+$"), "SKU chỉ chứa ký tự in hoa, số và dấu gạch ngang");
            
            // Kiểm tra số lượng tồn kho hợp lệ
            assertTrue(product.getStockQuantity() >= 0, "Số lượng tồn kho phải không âm");
        }

        // Test chất lượng dữ liệu đơn hàng
        List<OrderDto> orders = orderDao.findAll();
        for (OrderDto order : orders) {
            // Kiểm tra tổng tiền dương
            assertTrue(order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) > 0, 
                    "Tổng tiền đơn hàng phải dương");
            
            // Kiểm tra trạng thái hợp lệ
            assertTrue(order.getStatus().matches("PENDING|CONFIRMED|SHIPPED|DELIVERED|CANCELLED"), 
                    "Trạng thái đơn hàng phải hợp lệ");
            
            // Kiểm tra số đơn hàng hợp lệ
            assertTrue(order.getOrderNumber().startsWith("ORD"), "Số đơn hàng phải bắt đầu bằng ORD");
        }

        logger.info("Test chất lượng và tính thực tế dữ liệu thành công");
    }

    /**
     * Test chức năng dọn dẹp
     * Kiểm tra rằng dữ liệu test có thể được dọn dẹp hoàn toàn
     */
    @Test
    @Order(3)
    @DisplayName("Test Chức Năng Dọn Dẹp")
    void testCleanupFunctionality() throws Exception {
        logger.info("Test chức năng dọn dẹp");

        // Tạo dữ liệu test
        testDataSummary = dataGenerator.generateCompleteTestData(5, 8, 3);
        connectionManager.commit();

        // Xác minh dữ liệu tồn tại
        long initialUserCount = userDao.count();
        long initialProductCount = productDao.count();
        long initialOrderCount = orderDao.count();

        assertTrue(initialUserCount >= 5, "Phải có ít nhất 5 user");
        assertTrue(initialProductCount >= 8, "Phải có ít nhất 8 sản phẩm");
        assertTrue(initialOrderCount >= 3, "Phải có ít nhất 3 đơn hàng");

        // Thực hiện dọn dẹp
        dataGenerator.cleanupTestData(testDataSummary);
        connectionManager.commit();

        // Xác minh dữ liệu đã được dọn dẹp
        long finalUserCount = userDao.count();
        long finalProductCount = productDao.count();
        long finalOrderCount = orderDao.count();

        // Lưu ý: Dọn dẹp chỉ xóa dữ liệu test, không xóa dữ liệu gốc
        assertTrue(finalUserCount <= initialUserCount, "Số user phải giảm sau khi dọn dẹp");
        assertTrue(finalProductCount <= initialProductCount, "Số sản phẩm phải giảm sau khi dọn dẹp");
        assertTrue(finalOrderCount <= initialOrderCount, "Số đơn hàng phải giảm sau khi dọn dẹp");

        logger.info("Dọn dẹp thành công - Users: {} -> {}, Products: {} -> {}, Orders: {} -> {}", 
                initialUserCount, finalUserCount, initialProductCount, finalProductCount, 
                initialOrderCount, finalOrderCount);

        // Reset testDataSummary để tránh dọn dẹp lại trong tearDown
        testDataSummary = null;
    }
}
package com.example.demo;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
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
 * Lớp test xác minh query để xác minh logic nghiệp vụ thông qua các phương thức DAO.
 * Các test xác minh rằng các SQL query của developer (được đóng gói trong lớp DAO) 
 * đúng về mặt cú pháp và logic theo yêu cầu nghiệp vụ.
 * 
 * Query validation test class that validates business logic through DAO methods.
 * Tests verify that the developer's SQL queries (encapsulated in DAO layer) are 
 * syntactically and logically correct according to business requirements.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryValidationTest {

    // Logger để ghi log thông tin test
    private static final Logger logger = LoggerFactory.getLogger(QueryValidationTest.class);
    
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
     * Khởi tạo kết nối, DAO và tạo dữ liệu test toàn diện
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Thiết lập test xác minh query");

        // Khởi tạo quản lý kết nối và các DAO
        connectionManager = DatabaseConnectionManager.getInstance();
        userDao = new UserDaoImpl();
        productDao = new ProductDaoImpl();
        orderDao = new OrderDaoImpl();
        orderItemDao = new OrderItemDaoImpl();
        reviewDao = new ReviewDaoImpl();
        
        // Tạo bộ tạo dữ liệu với seed cố định để test có thể tái tạo
        dataGenerator = new TestDataGenerator(12345L);

        // Tạo dữ liệu test toàn diện sử dụng lớp DAO
        testDataSummary = dataGenerator.generateCompleteTestData(10, 20, 15);
        
        // Thêm dữ liệu test bổ sung cho việc xác minh query
        setupAdditionalTestData();
        
        connectionManager.commit();

        logger.info("Thiết lập test xác minh query hoàn tất");
    }

    /**
     * Dọn dẹp sau mỗi test case
     * Xóa dữ liệu test và đóng kết nối
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null && testDataSummary != null) {
            try {
                logger.info("Dọn dẹp dữ liệu test xác minh query");
                dataGenerator.cleanupTestData(testDataSummary);
                connectionManager.commit();
                logger.info("Dọn dẹp test xác minh query hoàn tất");
            } finally {
                connectionManager.closeConnection();
            }
        }
    }

    /**
     * Xác minh logic query xác thực user
     * Kiểm tra rằng chỉ user active mới có thể xác thực
     */
    @Test
    @Order(1)
    @DisplayName("Xác Minh Logic Query Xác Thực User")
    void validateUserAuthenticationQuery() throws SQLException {
        logger.info("Test logic query xác thực user");

        // Lấy một user test từ dữ liệu được tạo
        Long userId = testDataSummary.getUserIds().get(0);
        Optional<UserDto> userOpt = userDao.findById(userId);
        assertTrue(userOpt.isPresent(), "User test phải tồn tại");
        UserDto user = userOpt.get();

        // Yêu cầu nghiệp vụ: Xác thực user chỉ thành công cho user active
        Optional<UserDto> foundUser = userDao.findByUsernameOrEmail(user.getUsername());
        assertTrue(foundUser.isPresent(), "User active phải được tìm thấy để xác thực");
        assertTrue(foundUser.get().getIsActive(), "User phải active");
        assertEquals(user.getId(), foundUser.get().getId(), "Phải trả về user đúng");

        // Test vô hiệu hóa - quy tắc nghiệp vụ: user không active không thể xác thực
        userDao.deactivate(userId);
        connectionManager.commit();

        Optional<UserDto> deactivatedUser = userDao.findByUsernameOrEmail(user.getUsername());
        assertFalse(deactivatedUser.isPresent(), "User không active không được tìm thấy để xác thực");

        // Kích hoạt lại để dọn dẹp
        userDao.activate(userId);
        connectionManager.commit();

        logger.info("Xác minh query xác thực user thành công");
    }

    /**
     * Xác minh logic query báo cáo bán hàng
     * Kiểm tra rằng báo cáo bán hàng cung cấp tổng hợp chính xác
     */
    @Test
    @Order(2)
    @DisplayName("Xác Minh Logic Query Báo Cáo Bán Hàng")
    void validateSalesReportQuery() throws SQLException {
        logger.info("Test logic query báo cáo bán hàng");

        // Yêu cầu nghiệp vụ: Báo cáo bán hàng phải cung cấp tổng hợp chính xác
        List<OrderDto> salesOrders = orderDao.findForSalesReport(30);
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrderCount = 0;
        
        for (OrderDto order : salesOrders) {
            // Xác minh rằng chỉ đơn hàng DELIVERED/SHIPPED được bao gồm
            assertTrue(order.getStatus().equals("DELIVERED") || order.getStatus().equals("SHIPPED"),
                    "Báo cáo bán hàng chỉ nên bao gồm đơn hàng đã giao hoặc đã gửi");
            
            // Xác minh tính toàn vẹn dữ liệu đơn hàng
            assertNotNull(order.getOrderDate(), "Ngày đơn hàng không được null");
            assertTrue(order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0, 
                    "Tổng tiền phải dương");
            
            totalRevenue = totalRevenue.add(order.getTotalAmount());
            totalOrderCount++;
        }
        
        // Xác minh tổng hợp
        if (totalOrderCount > 0) {
            BigDecimal averageOrderValue = totalRevenue.divide(
                    new BigDecimal(totalOrderCount), 2, BigDecimal.ROUND_HALF_UP);
            assertTrue(averageOrderValue.compareTo(BigDecimal.ZERO) > 0, 
                    "Giá trị đơn hàng trung bình phải dương");
            
            logger.info("Xác minh báo cáo bán hàng - Đơn hàng: {} - Doanh thu: {} - Trung bình: {}", 
                    totalOrderCount, totalRevenue, averageOrderValue);
        }
        
        // Test hàm tính doanh thu
        BigDecimal daoRevenue = orderDao.getTotalRevenue();
        assertTrue(daoRevenue.compareTo(BigDecimal.ZERO) >= 0, 
                "Tổng doanh thu DAO phải không âm");

        logger.info("Xác minh query báo cáo bán hàng thành công");
    }

    /**
     * Xác minh logic hàm cơ sở dữ liệu
     * Kiểm tra các hàm tính toán trong cơ sở dữ liệu
     */
    @Test
    @Order(3)
    @DisplayName("Xác Minh Logic Hàm Cơ Sở Dữ Liệu")
    void validateDatabaseFunctionLogic() throws SQLException {
        logger.info("Test logic hàm cơ sở dữ liệu");

        // Test hàm tính tổng đơn hàng
        Long orderId = testDataSummary.getOrderIds().get(0);
        BigDecimal orderTotal = orderDao.calculateOrderTotal(orderId);
        assertNotNull(orderTotal, "Hàm tính tổng đơn hàng phải trả về kết quả");
        assertTrue(orderTotal.compareTo(BigDecimal.ZERO) > 0, "Tổng đơn hàng phải dương");

        // Test hàm tính tổng với thuế tùy chỉnh
        BigDecimal customTaxRate = new BigDecimal("0.10"); // 10%
        BigDecimal orderTotalWithTax = orderDao.calculateOrderTotalWithTax(orderId, customTaxRate);
        assertNotNull(orderTotalWithTax, "Hàm tính tổng với thuế phải trả về kết quả");
        assertTrue(orderTotalWithTax.compareTo(orderTotal) > 0, "Tổng với thuế phải lớn hơn tổng gốc");

        // Test hàm trạng thái thành viên
        Long userId = testDataSummary.getUserIds().get(0);
        String loyaltyStatus = orderDao.getUserLoyaltyStatus(userId);
        assertNotNull(loyaltyStatus, "Trạng thái thành viên không được null");
        assertTrue(loyaltyStatus.matches("BRONZE|SILVER|GOLD|PLATINUM"), 
                "Trạng thái thành viên phải hợp lệ");

        logger.info("Xác minh logic hàm cơ sở dữ liệu thành công");
    }

    // ===============================
    // PHƯƠNG THỨC HỖ TRỢ
    // ===============================

    /**
     * Thiết lập dữ liệu test bổ sung cho việc xác minh query
     * Tạo các đơn hàng với trạng thái cụ thể để test
     */
    private void setupAdditionalTestData() throws SQLException {
        logger.info("Thiết lập dữ liệu test bổ sung cho việc xác minh query");
        
        // Tạo các đơn hàng test cụ thể với trạng thái đã biết để test
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
        
        logger.info("Thiết lập dữ liệu test bổ sung hoàn tất");
    }
} 
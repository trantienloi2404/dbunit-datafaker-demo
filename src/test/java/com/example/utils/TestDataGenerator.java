package com.example.utils;

import com.example.demo.dao.*;
import com.example.demo.dao.impl.*;
import com.example.demo.dto.*;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Lớp tiện ích để tạo dữ liệu test thực tế sử dụng DataFaker và lớp DAO.
 * Tạo dữ liệu test thông qua các phương thức DAO thay vì SQL trực tiếp hoặc dataset DBUnit.
 * 
 * Utility class for generating realistic test data using DataFaker and DAO layer.
 * Creates test data through DAO methods instead of direct SQL or DBUnit datasets.
 */
public class TestDataGenerator {
    
    // Logger để ghi log thông tin
    private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);
    
    // Instance DataFaker để tạo dữ liệu thực tế
    private final Faker faker;
    // Random generator với seed cố định để có thể tái tạo
    private final Random random;
    
    // Các instance DAO để tương tác với cơ sở dữ liệu
    private final UserDao userDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ReviewDao reviewDao;
    private final DatabaseConnectionManager connectionManager;
    
    // Danh mục sản phẩm
    private static final String[] PRODUCT_CATEGORIES = {
        "Electronics", "Furniture", "Kitchen", "Sports", "Books", "Clothing", "Home & Garden", "Toys"
    };
    
    // Trạng thái đơn hàng
    private static final String[] ORDER_STATUSES = {
        "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
    };
    
    /**
     * Constructor mặc định với seed ngẫu nhiên
     */
    public TestDataGenerator() {
        this.faker = new Faker();
        this.random = new Random();
        this.userDao = new UserDaoImpl();
        this.productDao = new ProductDaoImpl();
        this.orderDao = new OrderDaoImpl();
        this.orderItemDao = new OrderItemDaoImpl();
        this.reviewDao = new ReviewDaoImpl();
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    /**
     * Constructor với seed cố định để có thể tái tạo dữ liệu
     * 
     * @param seed seed cho random generator
     */
    public TestDataGenerator(long seed) {
        this.faker = new Faker(new Random(seed));
        this.random = new Random(seed);
        this.userDao = new UserDaoImpl();
        this.productDao = new ProductDaoImpl();
        this.orderDao = new OrderDaoImpl();
        this.orderItemDao = new OrderItemDaoImpl();
        this.reviewDao = new ReviewDaoImpl();
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    /**
     * Tóm tắt dữ liệu chứa số lượng entity đã tạo và ID.
     */
    public static class TestDataSummary {
        private final List<Long> userIds = new ArrayList<>();
        private final List<Long> productIds = new ArrayList<>();
        private final List<Long> orderIds = new ArrayList<>();
        private final List<Long> orderItemIds = new ArrayList<>();
        private final List<Long> reviewIds = new ArrayList<>();
        
        public List<Long> getUserIds() { return userIds; }
        public List<Long> getProductIds() { return productIds; }
        public List<Long> getOrderIds() { return orderIds; }
        public List<Long> getOrderItemIds() { return orderItemIds; }
        public List<Long> getReviewIds() { return reviewIds; }
        
        public int getUserCount() { return userIds.size(); }
        public int getProductCount() { return productIds.size(); }
        public int getOrderCount() { return orderIds.size(); }
        public int getOrderItemCount() { return orderItemIds.size(); }
        public int getReviewCount() { return reviewIds.size(); }
        
        @Override
        public String toString() {
            return String.format("TestDataSummary{users=%d, products=%d, orders=%d, orderItems=%d, reviews=%d}",
                    getUserCount(), getProductCount(), getOrderCount(), getOrderItemCount(), getReviewCount());
        }
    }
    
    /**
     * Tạo một bộ dữ liệu test hoàn chỉnh với users, products, orders, order items và reviews.
     * 
     * @param userCount số lượng user cần tạo
     * @param productCount số lượng sản phẩm cần tạo
     * @param orderCount số lượng đơn hàng cần tạo
     * @return TestDataSummary chứa thông tin về dữ liệu đã tạo
     * @throws SQLException nếu thao tác cơ sở dữ liệu thất bại
     */
    public TestDataSummary generateCompleteTestData(int userCount, int productCount, int orderCount) throws SQLException {
        logger.info("Bắt đầu tạo dữ liệu test: {} users, {} products, {} orders", userCount, productCount, orderCount);
        
        TestDataSummary summary = new TestDataSummary();
        
        try {
            connectionManager.executeInTransaction(() -> {
                // Tạo các entity theo thứ tự phụ thuộc
                generateUsers(userCount, summary);
                generateProducts(productCount, summary);
                generateOrders(orderCount, summary);
                generateOrderItems(summary);
                generateReviews(Math.min(userCount * 2, 50), summary);
            });
            
            logger.info("Tạo dữ liệu test hoàn tất: {}", summary);
            return summary;
        } catch (SQLException e) {
            logger.error("Thất bại khi tạo dữ liệu test", e);
            throw e;
        }
    }
    
    /**
     * Tạo users sử dụng UserDao.
     * 
     * @param count số lượng user cần tạo
     * @param summary tóm tắt dữ liệu để lưu ID
     */
    private void generateUsers(int count, TestDataSummary summary) throws SQLException {
        Set<String> usedUsernames = new HashSet<>();
        Set<String> usedEmails = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            UserDto user = new UserDto();
            user.setUsername(generateUniqueUsername(usedUsernames));
            user.setEmail(generateUniqueEmail(usedEmails));
            user.setFirstName(faker.name().firstName());
            user.setLastName(faker.name().lastName());
            user.setDateOfBirth(new Date(faker.date().birthday(18, 65).getTime()));
            user.setPhoneNumber(faker.phoneNumber().phoneNumber());
            user.setIsActive(true);
            
            UserDto createdUser = userDao.create(user);
            summary.getUserIds().add(createdUser.getId());
        }
        
        logger.debug("Đã tạo {} users", count);
    }
    
    /**
     * Tạo sản phẩm sử dụng ProductDao.
     * 
     * @param count số lượng sản phẩm cần tạo
     * @param summary tóm tắt dữ liệu để lưu ID
     */
    private void generateProducts(int count, TestDataSummary summary) throws SQLException {
        Set<String> usedSkus = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            ProductDto product = new ProductDto();
            product.setName(faker.commerce().productName());
            product.setDescription(faker.lorem().paragraph(3));
            product.setPrice(new BigDecimal(faker.number().randomDouble(2, 10, 1000))
                    .setScale(2, RoundingMode.HALF_UP));
            product.setCategory(PRODUCT_CATEGORIES[random.nextInt(PRODUCT_CATEGORIES.length)]);
            product.setSku(generateUniqueSku(usedSkus));
            product.setStockQuantity(faker.number().numberBetween(0, 500));
            product.setIsAvailable(product.getStockQuantity() > 0 && faker.bool().bool());
            
            ProductDto createdProduct = productDao.create(product);
            summary.getProductIds().add(createdProduct.getId());
        }
        
        logger.debug("Đã tạo {} products", count);
    }
    
    /**
     * Tạo đơn hàng sử dụng OrderDao.
     * 
     * @param count số lượng đơn hàng cần tạo
     * @param summary tóm tắt dữ liệu để lưu ID
     */
    private void generateOrders(int count, TestDataSummary summary) throws SQLException {
        Set<String> usedOrderNumbers = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            OrderDto order = new OrderDto();
            
            // User ID ngẫu nhiên từ users đã tạo
            Long userId = summary.getUserIds().get(random.nextInt(summary.getUserIds().size()));
            order.setUserId(userId);
            order.setOrderNumber(generateUniqueOrderNumber(usedOrderNumbers));
            order.setTotalAmount(new BigDecimal(faker.number().randomDouble(2, 50, 2000))
                    .setScale(2, RoundingMode.HALF_UP));
            order.setStatus(ORDER_STATUSES[random.nextInt(ORDER_STATUSES.length)]);
            order.setDeliveryAddress(faker.address().fullAddress());
            
            // Đặt ngày gửi cho đơn hàng đã gửi/giao
            if ("SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
                order.setShippedDate(Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(30))));
            }
            
            OrderDto createdOrder = orderDao.create(order);
            summary.getOrderIds().add(createdOrder.getId());
        }
        
        logger.debug("Đã tạo {} orders", count);
    }
    
    /**
     * Tạo chi tiết đơn hàng cho các đơn hàng hiện có sử dụng OrderItemDao.
     * 
     * @param summary tóm tắt dữ liệu để lưu ID
     */
    private void generateOrderItems(TestDataSummary summary) throws SQLException {
        for (Long orderId : summary.getOrderIds()) {
            // Tạo 1-5 items cho mỗi đơn hàng
            int itemCount = random.nextInt(5) + 1;
            Set<Long> usedProductIds = new HashSet<>();
            
            for (int i = 0; i < itemCount; i++) {
                Long productId;
                do {
                    productId = summary.getProductIds().get(random.nextInt(summary.getProductIds().size()));
                } while (usedProductIds.contains(productId) && usedProductIds.size() < summary.getProductIds().size());
                
                usedProductIds.add(productId);
                
                OrderItemDto orderItem = new OrderItemDto();
                orderItem.setOrderId(orderId);
                orderItem.setProductId(productId);
                orderItem.setQuantity(random.nextInt(5) + 1);
                orderItem.setUnitPrice(new BigDecimal(faker.number().randomDouble(2, 10, 500))
                        .setScale(2, RoundingMode.HALF_UP));
                orderItem.setTotalPrice(orderItem.getUnitPrice().multiply(new BigDecimal(orderItem.getQuantity())));
                
                OrderItemDto createdOrderItem = orderItemDao.create(orderItem);
                summary.getOrderItemIds().add(createdOrderItem.getId());
            }
        }
        
        logger.debug("Đã tạo {} order items", summary.getOrderItemIds().size());
    }
    
    /**
     * Tạo đánh giá sử dụng ReviewDao.
     * 
     * @param count số lượng đánh giá cần tạo
     * @param summary tóm tắt dữ liệu để lưu ID
     */
    private void generateReviews(int count, TestDataSummary summary) throws SQLException {
        Set<String> usedUserProductPairs = new HashSet<>();
        
        for (int i = 0; i < count && usedUserProductPairs.size() < summary.getUserIds().size() * summary.getProductIds().size(); i++) {
            Long userId = summary.getUserIds().get(random.nextInt(summary.getUserIds().size()));
            Long productId = summary.getProductIds().get(random.nextInt(summary.getProductIds().size()));
            String pairKey = userId + ":" + productId;
            
            if (usedUserProductPairs.contains(pairKey)) {
                i--; // Thử lại với cặp khác
                continue;
            }
            
            usedUserProductPairs.add(pairKey);
            
            ReviewDto review = new ReviewDto();
            review.setUserId(userId);
            review.setProductId(productId);
            review.setRating(random.nextInt(5) + 1);
            review.setTitle(faker.lorem().sentence(5));
            review.setComment(faker.lorem().paragraph(2));
            review.setIsVerifiedPurchase(faker.bool().bool());
            
            ReviewDto createdReview = reviewDao.create(review);
            summary.getReviewIds().add(createdReview.getId());
        }
        
        logger.debug("Đã tạo {} reviews", summary.getReviewIds().size());
    }
    
    /**
     * Dọn dẹp tất cả dữ liệu test đã tạo.
     * 
     * @param summary tóm tắt dữ liệu cần dọn dẹp
     */
    public void cleanupTestData(TestDataSummary summary) throws SQLException {
        logger.info("Dọn dẹp dữ liệu test: {}", summary);
        
        try {
            connectionManager.executeInTransaction(() -> {
                // Xóa theo thứ tự phụ thuộc ngược
                for (Long reviewId : summary.getReviewIds()) {
                    reviewDao.delete(reviewId);
                }
                
                for (Long orderItemId : summary.getOrderItemIds()) {
                    orderItemDao.delete(orderItemId);
                }
                
                for (Long orderId : summary.getOrderIds()) {
                    orderDao.delete(orderId);
                }
                
                for (Long productId : summary.getProductIds()) {
                    productDao.delete(productId);
                }
                
                for (Long userId : summary.getUserIds()) {
                    userDao.delete(userId);
                }
            });
            
            logger.info("Dọn dẹp dữ liệu test hoàn tất");
        } catch (SQLException e) {
            logger.error("Thất bại khi dọn dẹp dữ liệu test", e);
            throw e;
        }
    }
    
    // Phương thức tiện ích cho việc tạo dữ liệu cụ thể
    
    /**
     * Chỉ tạo users.
     * 
     * @param count số lượng user cần tạo
     * @return danh sách user đã tạo
     */
    public List<UserDto> generateUsers(int count) throws SQLException {
        List<UserDto> users = new ArrayList<>();
        Set<String> usedUsernames = new HashSet<>();
        Set<String> usedEmails = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            UserDto user = new UserDto();
            user.setUsername(generateUniqueUsername(usedUsernames));
            user.setEmail(generateUniqueEmail(usedEmails));
            user.setFirstName(faker.name().firstName());
            user.setLastName(faker.name().lastName());
            user.setDateOfBirth(new Date(faker.date().birthday(18, 65).getTime()));
            user.setPhoneNumber(faker.phoneNumber().phoneNumber());
            user.setIsActive(true);
            
            users.add(userDao.create(user));
        }
        
        return users;
    }
    
    /**
     * Chỉ tạo sản phẩm.
     * 
     * @param count số lượng sản phẩm cần tạo
     * @return danh sách sản phẩm đã tạo
     */
    public List<ProductDto> generateProducts(int count) throws SQLException {
        List<ProductDto> products = new ArrayList<>();
        Set<String> usedSkus = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            ProductDto product = new ProductDto();
            product.setName(faker.commerce().productName());
            product.setDescription(faker.lorem().paragraph(3));
            product.setPrice(new BigDecimal(faker.number().randomDouble(2, 10, 1000))
                    .setScale(2, RoundingMode.HALF_UP));
            product.setCategory(PRODUCT_CATEGORIES[random.nextInt(PRODUCT_CATEGORIES.length)]);
            product.setSku(generateUniqueSku(usedSkus));
            product.setStockQuantity(faker.number().numberBetween(0, 500));
            product.setIsAvailable(product.getStockQuantity() > 0 && faker.bool().bool());
            
            products.add(productDao.create(product));
        }
        
        return products;
    }
    
    // Phương thức hỗ trợ
    
    /**
     * Tạo username duy nhất.
     * 
     * @param usedUsernames tập hợp username đã sử dụng
     * @return username duy nhất
     */
    private String generateUniqueUsername(Set<String> usedUsernames) {
        String username;
        do {
            username = faker.internet().username();
        } while (usedUsernames.contains(username));
        usedUsernames.add(username);
        return username;
    }
    
    /**
     * Tạo email duy nhất.
     * 
     * @param usedEmails tập hợp email đã sử dụng
     * @return email duy nhất
     */
    private String generateUniqueEmail(Set<String> usedEmails) {
        String email;
        do {
            email = faker.internet().emailAddress();
        } while (usedEmails.contains(email));
        usedEmails.add(email);
        return email;
    }
    
    /**
     * Tạo SKU duy nhất.
     * 
     * @param usedSkus tập hợp SKU đã sử dụng
     * @return SKU duy nhất
     */
    private String generateUniqueSku(Set<String> usedSkus) {
        String sku;
        do {
            sku = faker.code().asin();
        } while (usedSkus.contains(sku));
        usedSkus.add(sku);
        return sku;
    }
    
    /**
     * Tạo số đơn hàng duy nhất.
     * 
     * @param usedOrderNumbers tập hợp số đơn hàng đã sử dụng
     * @return số đơn hàng duy nhất
     */
    private String generateUniqueOrderNumber(Set<String> usedOrderNumbers) {
        String orderNumber;
        do {
            orderNumber = "ORD-" + faker.number().digits(8);
        } while (usedOrderNumbers.contains(orderNumber));
        usedOrderNumbers.add(orderNumber);
        return orderNumber;
    }
} 
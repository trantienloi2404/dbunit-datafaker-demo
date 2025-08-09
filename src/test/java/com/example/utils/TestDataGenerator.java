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
 * Utility for generating realistic test data using DataFaker and the DAO layer.
 * Creates test data via DAO methods instead of direct SQL or DBUnit datasets.
 */
public class TestDataGenerator {
    
    // Logger for information
    private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);
    
    // DataFaker instance for realistic data
    private final Faker faker;
    // Random generator, optionally with fixed seed for reproducibility
    private final Random random;
    
    // DAO instances to interact with the database
    private final UserDao userDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final ReviewDao reviewDao;
    private final DatabaseConnectionManager connectionManager;
    
    // Product categories
    private static final String[] PRODUCT_CATEGORIES = {
        "Electronics", "Furniture", "Kitchen", "Sports", "Books", "Clothing", "Home & Garden", "Toys"
    };
    
    // Order statuses
    private static final String[] ORDER_STATUSES = {
        "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
    };
    
    /** Default constructor with random seed */
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
    
    /** Constructor with fixed seed for reproducible data */
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
     * Summary of generated data: entity counts and IDs.
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
     * Generates a complete test dataset with users, products, orders, order items and reviews.
     *
     * @param userCount number of users to create
     * @param productCount number of products to create
     * @param orderCount number of orders to create
     * @return TestDataSummary containing details of generated data
     * @throws SQLException if any database operation fails
     */
    public TestDataSummary generateCompleteTestData(int userCount, int productCount, int orderCount) throws SQLException {
        logger.info("Generating test data: {} users, {} products, {} orders", userCount, productCount, orderCount);
        
        TestDataSummary summary = new TestDataSummary();
        
        try {
            connectionManager.executeInTransaction(() -> {
                // Create entities in dependency order
                generateUsers(userCount, summary);
                generateProducts(productCount, summary);
                generateOrders(orderCount, summary);
                generateOrderItems(summary);
                generateReviews(Math.min(userCount * 2, 50), summary);
            });
            
            logger.info("Finished generating test data: {}", summary);
            return summary;
        } catch (SQLException e) {
            logger.error("Failed to generate test data", e);
            throw e;
        }
    }
    
    /**
     * Creates users using UserDao.
     *
     * @param count number of users to create
     * @param summary data summary to collect generated IDs
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
        
        logger.debug("Created {} users", count);
    }
    
    /**
     * Creates products using ProductDao.
     *
     * @param count number of products to create
     * @param summary data summary to collect generated IDs
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
        
        logger.debug("Created {} products", count);
    }
    
    /**
     * Creates orders using OrderDao.
     *
     * @param count number of orders to create
     * @param summary data summary to collect generated IDs
     */
    private void generateOrders(int count, TestDataSummary summary) throws SQLException {
        Set<String> usedOrderNumbers = new HashSet<>();
        
        for (int i = 0; i < count; i++) {
            OrderDto order = new OrderDto();
            
            // Pick a random user ID from created users
            Long userId = summary.getUserIds().get(random.nextInt(summary.getUserIds().size()));
            order.setUserId(userId);
            order.setOrderNumber(generateUniqueOrderNumber(usedOrderNumbers));
            order.setTotalAmount(new BigDecimal(faker.number().randomDouble(2, 50, 2000))
                    .setScale(2, RoundingMode.HALF_UP));
            order.setStatus(ORDER_STATUSES[random.nextInt(ORDER_STATUSES.length)]);
            order.setDeliveryAddress(faker.address().fullAddress());
            
            // Set shipped date for shipped/delivered orders
            if ("SHIPPED".equals(order.getStatus()) || "DELIVERED".equals(order.getStatus())) {
                order.setShippedDate(Timestamp.valueOf(LocalDateTime.now().minusDays(random.nextInt(30))));
            }
            
            OrderDto createdOrder = orderDao.create(order);
            summary.getOrderIds().add(createdOrder.getId());
        }
        
        logger.debug("Created {} orders", count);
    }
    
    /**
     * Creates order items for existing orders using OrderItemDao.
     *
     * @param summary data summary to collect generated IDs
     */
    private void generateOrderItems(TestDataSummary summary) throws SQLException {
        for (Long orderId : summary.getOrderIds()) {
            // Create 1-5 items per order
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
        
        logger.debug("Created {} order items", summary.getOrderItemIds().size());
    }
    
    /**
     * Creates reviews using ReviewDao.
     *
     * @param count number of reviews to create
     * @param summary data summary to collect generated IDs
     */
    private void generateReviews(int count, TestDataSummary summary) throws SQLException {
        Set<String> usedUserProductPairs = new HashSet<>();
        
        for (int i = 0; i < count && usedUserProductPairs.size() < summary.getUserIds().size() * summary.getProductIds().size(); i++) {
            Long userId = summary.getUserIds().get(random.nextInt(summary.getUserIds().size()));
            Long productId = summary.getProductIds().get(random.nextInt(summary.getProductIds().size()));
            String pairKey = userId + ":" + productId;
            
            if (usedUserProductPairs.contains(pairKey)) {
                i--; // Retry with a different pair
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
        
        logger.debug("Created {} reviews", summary.getReviewIds().size());
    }
    
    /**
     * Cleans up all generated test data.
     *
     * @param summary summary of test data to clean up
     */
    public void cleanupTestData(TestDataSummary summary) throws SQLException {
        logger.info("Cleaning up test data: {}", summary);
        
        try {
            connectionManager.executeInTransaction(() -> {
                // Delete in reverse dependency order
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
            
            logger.info("Finished cleaning up test data");
        } catch (SQLException e) {
            logger.error("Failed to clean up test data", e);
            throw e;
        }
    }
    
    // Utility methods for generating specific data
    
    /**
     * Generates users only.
     *
     * @param count number of users to create
     * @return list of created users
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
     * Generates products only.
     *
     * @param count number of products to create
     * @return list of created products
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
    
    // Helper methods
    
    /**
     * Generates a unique username.
     *
     * @param usedUsernames set of already used usernames
     * @return unique username
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
     * Generates a unique email address.
     *
     * @param usedEmails set of already used emails
     * @return unique email
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
     * Generates a unique SKU.
     *
     * @param usedSkus set of already used SKUs
     * @return unique SKU
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
     * Generates a unique order number.
     *
     * @param usedOrderNumbers set of already used order numbers
     * @return unique order number
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
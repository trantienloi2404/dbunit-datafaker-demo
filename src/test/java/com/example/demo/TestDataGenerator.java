package com.example.demo;

import net.datafaker.Faker;
import org.dbunit.dataset.DefaultDataSet;
import org.dbunit.dataset.DefaultTable;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.Column;
import org.dbunit.dataset.datatype.DataType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for generating realistic test data using DataFaker.
 * Creates DBUnit datasets with fake but realistic data for testing.
 */
public class TestDataGenerator {
    
    private final Faker faker;
    private final Random random;
    
    // Categories for products
    private static final String[] PRODUCT_CATEGORIES = {
        "Electronics", "Furniture", "Kitchen", "Sports", "Books", "Clothing", "Home & Garden", "Toys"
    };
    
    // Order statuses
    private static final String[] ORDER_STATUSES = {
        "PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"
    };
    
    public TestDataGenerator() {
        this.faker = new Faker();
        this.random = new Random();
    }
    
    public TestDataGenerator(long seed) {
        this.faker = new Faker(new Random(seed));
        this.random = new Random(seed);
    }
    
    /**
     * Generates a complete dataset with users, products, orders, order items, and reviews.
     * 
     * @param userCount number of users to generate
     * @param productCount number of products to generate
     * @param orderCount number of orders to generate
     * @return IDataSet containing all generated tables
     * @throws Exception if dataset creation fails
     */
    public IDataSet generateCompleteDataSet(int userCount, int productCount, int orderCount) throws Exception {
        DefaultDataSet dataSet = new DefaultDataSet();
        
        // Generate tables in dependency order
        DefaultTable usersTable = generateUsersTable(userCount);
        DefaultTable productsTable = generateProductsTable(productCount);
        DefaultTable ordersTable = generateOrdersTable(orderCount, userCount);
        DefaultTable orderItemsTable = generateOrderItemsTable(ordersTable, productsTable);
        DefaultTable reviewsTable = generateReviewsTable(userCount, productCount, Math.min(userCount * 3, 50));
        
        dataSet.addTable(usersTable);
        dataSet.addTable(productsTable);
        dataSet.addTable(ordersTable);
        dataSet.addTable(orderItemsTable);
        dataSet.addTable(reviewsTable);
        
        return dataSet;
    }
    
    /**
     * Generates a users table with fake user data.
     * 
     * @param count number of users to generate
     * @return DefaultTable containing user data
     * @throws org.dbunit.dataset.DataSetException if table operations fail
     */
    public DefaultTable generateUsersTable(int count) throws org.dbunit.dataset.DataSetException {
        Column[] columns = {
            new Column("id", DataType.BIGINT),
            new Column("username", DataType.VARCHAR),
            new Column("email", DataType.VARCHAR),
            new Column("first_name", DataType.VARCHAR),
            new Column("last_name", DataType.VARCHAR),
            new Column("date_of_birth", DataType.DATE),
            new Column("phone_number", DataType.VARCHAR),
            new Column("created_at", DataType.TIMESTAMP),
            new Column("updated_at", DataType.TIMESTAMP),
            new Column("is_active", DataType.BOOLEAN)
        };
        
        DefaultTable table = new DefaultTable("users", columns);
        Set<String> usedUsernames = new HashSet<>();
        Set<String> usedEmails = new HashSet<>();
        
        for (int i = 1; i <= count; i++) {
            String username = generateUniqueUsername(usedUsernames);
            String email = generateUniqueEmail(usedEmails);
            String firstName = faker.name().firstName();
            String lastName = faker.name().lastName();
            Date dateOfBirth = new Date(faker.date().birthday(18, 65).getTime());
            String phoneNumber = faker.phoneNumber().phoneNumber();
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            
            Object[] row = {
                (long) i,
                username,
                email,
                firstName,
                lastName,
                dateOfBirth,
                phoneNumber,
                now,
                now,
                1  // Use 1 instead of true for MySQL compatibility
            };
            
            table.addRow(row);
        }
        
        return table;
    }
    
    /**
     * Generates a products table with fake product data.
     * 
     * @param count number of products to generate
     * @return DefaultTable containing product data
     * @throws org.dbunit.dataset.DataSetException if table operations fail
     */
    public DefaultTable generateProductsTable(int count) throws org.dbunit.dataset.DataSetException {
        Column[] columns = {
            new Column("id", DataType.BIGINT),
            new Column("name", DataType.VARCHAR),
            new Column("description", DataType.LONGVARCHAR),
            new Column("price", DataType.DECIMAL),
            new Column("category", DataType.VARCHAR),
            new Column("sku", DataType.VARCHAR),
            new Column("stock_quantity", DataType.INTEGER),
            new Column("is_available", DataType.BOOLEAN),
            new Column("created_at", DataType.TIMESTAMP),
            new Column("updated_at", DataType.TIMESTAMP)
        };
        
        DefaultTable table = new DefaultTable("products", columns);
        Set<String> usedSkus = new HashSet<>();
        
        for (int i = 1; i <= count; i++) {
            String name = faker.commerce().productName();
            String description = faker.lorem().paragraph(3);
            BigDecimal price = new BigDecimal(faker.number().randomDouble(2, 10, 1000))
                    .setScale(2, RoundingMode.HALF_UP);
            String category = PRODUCT_CATEGORIES[random.nextInt(PRODUCT_CATEGORIES.length)];
            String sku = generateUniqueSku(usedSkus);
            int stockQuantity = faker.number().numberBetween(0, 500);
            boolean isAvailable = stockQuantity > 0 && faker.bool().bool();
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            
            Object[] row = {
                (long) i,
                name,
                description,
                price,
                category,
                sku,
                stockQuantity,
                isAvailable ? 1 : 0,  // Convert boolean to int for MySQL
                now,
                now
            };
            
            table.addRow(row);
        }
        
        return table;
    }
    
    /**
     * Generates an orders table with fake order data.
     * 
     * @param count number of orders to generate
     * @param userCount number of available users
     * @return DefaultTable containing order data
     * @throws org.dbunit.dataset.DataSetException if table operations fail
     */
    public DefaultTable generateOrdersTable(int count, int userCount) throws org.dbunit.dataset.DataSetException {
        Column[] columns = {
            new Column("id", DataType.BIGINT),
            new Column("user_id", DataType.BIGINT),
            new Column("order_number", DataType.VARCHAR),
            new Column("total_amount", DataType.DECIMAL),
            new Column("status", DataType.VARCHAR),
            new Column("order_date", DataType.TIMESTAMP),
            new Column("shipped_date", DataType.TIMESTAMP),
            new Column("delivery_address", DataType.LONGVARCHAR)
        };
        
        DefaultTable table = new DefaultTable("orders", columns);
        Set<String> usedOrderNumbers = new HashSet<>();
        
        for (int i = 1; i <= count; i++) {
            long userId = faker.number().numberBetween(1, userCount + 1);
            String orderNumber = generateUniqueOrderNumber(usedOrderNumbers);
            BigDecimal totalAmount = new BigDecimal(faker.number().randomDouble(2, 25, 2000))
                    .setScale(2, RoundingMode.HALF_UP);
            String status = ORDER_STATUSES[random.nextInt(ORDER_STATUSES.length)];
            
            LocalDateTime orderDate = faker.date().past(90, TimeUnit.DAYS).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime();
            Timestamp orderTimestamp = Timestamp.valueOf(orderDate);
            
            Timestamp shippedDate = null;
            if ("SHIPPED".equals(status) || "DELIVERED".equals(status)) {
                shippedDate = Timestamp.valueOf(orderDate.plusDays(faker.number().numberBetween(1, 5)));
            }
            
            String deliveryAddress = faker.address().fullAddress();
            
            Object[] row = {
                (long) i,
                userId,
                orderNumber,
                totalAmount,
                status,
                orderTimestamp,
                shippedDate,
                deliveryAddress
            };
            
            table.addRow(row);
        }
        
        return table;
    }
    
    /**
     * Generates order items table based on existing orders and products.
     * 
     * @param ordersTable the orders table to generate items for
     * @param productsTable the products table to select from
     * @return DefaultTable containing order items data
     * @throws org.dbunit.dataset.DataSetException if table operations fail
     */
    public DefaultTable generateOrderItemsTable(DefaultTable ordersTable, DefaultTable productsTable) throws org.dbunit.dataset.DataSetException {
        Column[] columns = {
            new Column("id", DataType.BIGINT),
            new Column("order_id", DataType.BIGINT),
            new Column("product_id", DataType.BIGINT),
            new Column("quantity", DataType.INTEGER),
            new Column("unit_price", DataType.DECIMAL),
            new Column("total_price", DataType.DECIMAL)
        };
        
        DefaultTable table = new DefaultTable("order_items", columns);
        int itemId = 1;
        
        for (int i = 0; i < ordersTable.getRowCount(); i++) {
            long orderId = (Long) ordersTable.getValue(i, "id");
            int itemCount = faker.number().numberBetween(1, 5); // 1-4 items per order
            
            Set<Long> usedProductIds = new HashSet<>();
            
            for (int j = 0; j < itemCount; j++) {
                long productId;
                do {
                    productId = faker.number().numberBetween(1, productsTable.getRowCount() + 1);
                } while (usedProductIds.contains(productId));
                usedProductIds.add(productId);
                
                int quantity = faker.number().numberBetween(1, 4);
                BigDecimal unitPrice = new BigDecimal(faker.number().randomDouble(2, 10, 500))
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity))
                        .setScale(2, RoundingMode.HALF_UP);
                
                Object[] row = {
                    (long) itemId++,
                    orderId,
                    productId,
                    quantity,
                    unitPrice,
                    totalPrice
                };
                
                table.addRow(row);
            }
        }
        
        return table;
    }
    
    /**
     * Generates a reviews table with fake review data.
     * 
     * @param userCount number of available users
     * @param productCount number of available products
     * @param reviewCount number of reviews to generate
     * @return DefaultTable containing review data
     * @throws org.dbunit.dataset.DataSetException if table operations fail
     */
    public DefaultTable generateReviewsTable(int userCount, int productCount, int reviewCount) throws org.dbunit.dataset.DataSetException {
        Column[] columns = {
            new Column("id", DataType.BIGINT),
            new Column("user_id", DataType.BIGINT),
            new Column("product_id", DataType.BIGINT),
            new Column("rating", DataType.INTEGER),
            new Column("title", DataType.VARCHAR),
            new Column("comment", DataType.LONGVARCHAR),
            new Column("review_date", DataType.TIMESTAMP),
            new Column("is_verified_purchase", DataType.BOOLEAN)
        };
        
        DefaultTable table = new DefaultTable("reviews", columns);
        Set<String> usedUserProductCombos = new HashSet<>();
        
        for (int i = 1; i <= reviewCount; i++) {
            long userId;
            long productId;
            String combo;
            
            // Ensure unique user-product combinations
            do {
                userId = faker.number().numberBetween(1, userCount + 1);
                productId = faker.number().numberBetween(1, productCount + 1);
                combo = userId + "-" + productId;
            } while (usedUserProductCombos.contains(combo));
            
            usedUserProductCombos.add(combo);
            
            int rating = faker.number().numberBetween(1, 6); // 1-5 stars
            String title = faker.lorem().sentence(4, 6);
            String comment = faker.lorem().paragraph(2);
            Timestamp reviewDate = Timestamp.valueOf(
                faker.date().past(30, TimeUnit.DAYS).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDateTime());
            boolean isVerifiedPurchase = faker.bool().bool();
            
            Object[] row = {
                (long) i,
                userId,
                productId,
                rating,
                title,
                comment,
                reviewDate,
                isVerifiedPurchase ? 1 : 0  // Convert boolean to int for MySQL
            };
            
            table.addRow(row);
        }
        
        return table;
    }
    
    // Helper methods
    
    private String generateUniqueUsername(Set<String> usedUsernames) {
        String username;
        do {
            username = faker.internet().username();
        } while (usedUsernames.contains(username));
        usedUsernames.add(username);
        return username;
    }
    
    private String generateUniqueEmail(Set<String> usedEmails) {
        String email;
        do {
            email = faker.internet().emailAddress();
        } while (usedEmails.contains(email));
        usedEmails.add(email);
        return email;
    }
    
    private String generateUniqueSku(Set<String> usedSkus) {
        String sku;
        do {
            sku = faker.code().asin();
        } while (usedSkus.contains(sku));
        usedSkus.add(sku);
        return sku;
    }
    
    private String generateUniqueOrderNumber(Set<String> usedOrderNumbers) {
        String orderNumber;
        do {
            orderNumber = "ORD-" + faker.number().digits(8);
        } while (usedOrderNumbers.contains(orderNumber));
        usedOrderNumbers.add(orderNumber);
        return orderNumber;
    }
} 
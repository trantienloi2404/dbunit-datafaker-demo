-- Create database schema for DBUnit and DataFaker demo

-- Users table
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE,
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Products table
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(50) NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    stock_quantity INT DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED') DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shipped_date TIMESTAMP NULL,
    delivery_address TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Order items table
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Reviews table
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(200),
    comment TEXT,
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_verified_purchase BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY unique_user_product_review (user_id, product_id)
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_rating ON reviews(rating);

-- =====================================
-- STORED PROCEDURES, FUNCTIONS, AND TRIGGERS
-- =====================================

-- Function: Calculate total order value including tax (default 8.75%)
DELIMITER //
CREATE FUNCTION calculate_order_total(order_id_param BIGINT)
RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_amount DECIMAL(10,2) DEFAULT 0.00;
    DECLARE tax_rate DECIMAL(5,4) DEFAULT 0.0875;
    
    SELECT COALESCE(SUM(total_price), 0.00)
    INTO total_amount
    FROM order_items
    WHERE order_id = order_id_param;
    
    RETURN total_amount * (1 + tax_rate);
END //
DELIMITER ;

-- Function: Calculate total order value with custom tax rate
DELIMITER //
CREATE FUNCTION calculate_order_total_with_tax(order_id_param BIGINT, tax_rate DECIMAL(5,4))
RETURNS DECIMAL(10,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE total_amount DECIMAL(10,2) DEFAULT 0.00;
    
    SELECT COALESCE(SUM(total_price), 0.00)
    INTO total_amount
    FROM order_items
    WHERE order_id = order_id_param;
    
    RETURN total_amount * (1 + tax_rate);
END //
DELIMITER ;

-- Function: Get user loyalty status based on order history
DELIMITER //
CREATE FUNCTION get_user_loyalty_status(user_id_param BIGINT)
RETURNS VARCHAR(20)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE order_count INT DEFAULT 0;
    DECLARE total_spent DECIMAL(10,2) DEFAULT 0.00;
    DECLARE loyalty_status VARCHAR(20) DEFAULT 'BRONZE';
    
    SELECT COUNT(*), COALESCE(SUM(total_amount), 0.00)
    INTO order_count, total_spent
    FROM orders
    WHERE user_id = user_id_param AND status IN ('DELIVERED', 'SHIPPED');
    
    IF total_spent >= 5000.00 OR order_count >= 20 THEN
        SET loyalty_status = 'PLATINUM';
    ELSEIF total_spent >= 2000.00 OR order_count >= 10 THEN
        SET loyalty_status = 'GOLD';
    ELSEIF total_spent >= 500.00 OR order_count >= 5 THEN
        SET loyalty_status = 'SILVER';
    END IF;
    
    RETURN loyalty_status;
END //
DELIMITER ;

-- Function: Calculate average product rating
DELIMITER //
CREATE FUNCTION get_product_rating(product_id_param BIGINT)
RETURNS DECIMAL(3,2)
READS SQL DATA
DETERMINISTIC
BEGIN
    DECLARE avg_rating DECIMAL(3,2) DEFAULT 0.00;
    
    SELECT COALESCE(AVG(rating), 0.00)
    INTO avg_rating
    FROM reviews
    WHERE product_id = product_id_param;
    
    RETURN avg_rating;
END //
DELIMITER ;

-- Stored Procedure: Process order shipment
DELIMITER //
CREATE PROCEDURE process_order_shipment(
    IN order_id_param BIGINT,
    IN tracking_number VARCHAR(100)
)
BEGIN
    DECLARE current_status VARCHAR(20);
    DECLARE order_exists INT DEFAULT 0;
    
    -- Check if order exists and get current status
    SELECT COUNT(*), COALESCE(MAX(status), '') 
    INTO order_exists, current_status
    FROM orders 
    WHERE id = order_id_param;
    
    -- Validate order exists
    IF order_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order not found';
    END IF;
    
    -- Validate current status
    IF current_status NOT IN ('PENDING', 'CONFIRMED') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order cannot be shipped from current status';
    END IF;
    
    -- Update order status and shipped date
    UPDATE orders 
    SET status = 'SHIPPED', 
        shipped_date = CURRENT_TIMESTAMP
    WHERE id = order_id_param;
    
    -- Log shipment (would insert into audit table in real system)
    SELECT CONCAT('Order ', order_id_param, ' shipped with tracking: ', tracking_number) as result;
END //
DELIMITER ;

-- Stored Procedure: Restock product
DELIMITER //
CREATE PROCEDURE restock_product(
    IN product_id_param BIGINT,
    IN quantity_to_add INT,
    IN restock_reason VARCHAR(200)
)
BEGIN
    DECLARE product_exists INT DEFAULT 0;
    DECLARE current_stock INT DEFAULT 0;
    
    -- Validate inputs
    IF quantity_to_add <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Quantity to add must be positive';
    END IF;
    
    -- Check if product exists
    SELECT COUNT(*), COALESCE(MAX(stock_quantity), 0)
    INTO product_exists, current_stock
    FROM products 
    WHERE id = product_id_param;
    
    IF product_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Product not found';
    END IF;
    
    -- Update stock quantity and availability
    UPDATE products 
    SET stock_quantity = stock_quantity + quantity_to_add,
        is_available = TRUE,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = product_id_param;
    
    -- Return updated stock information
    SELECT 
        id,
        name,
        (current_stock + quantity_to_add) as new_stock_quantity,
        quantity_to_add as quantity_added,
        restock_reason
    FROM products 
    WHERE id = product_id_param;
END //
DELIMITER ;

-- Stored Procedure: Get user order summary
DELIMITER //
CREATE PROCEDURE get_user_order_summary(IN user_id_param BIGINT)
BEGIN
    SELECT 
        u.username,
        u.email,
        get_user_loyalty_status(u.id) as loyalty_status,
        COUNT(o.id) as total_orders,
        COALESCE(SUM(o.total_amount), 0.00) as total_spent,
        COALESCE(AVG(o.total_amount), 0.00) as avg_order_value,
        MAX(o.order_date) as last_order_date
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.id = user_id_param
    GROUP BY u.id, u.username, u.email;
END //
DELIMITER ;

-- Trigger: Update product stock when order items are inserted
DELIMITER //
CREATE TRIGGER update_stock_on_order_insert
    AFTER INSERT ON order_items
    FOR EACH ROW
BEGIN
    UPDATE products 
    SET stock_quantity = stock_quantity - NEW.quantity,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.product_id;
    
    -- Set product unavailable if stock goes to zero or negative
    UPDATE products 
    SET is_available = FALSE
    WHERE id = NEW.product_id AND stock_quantity <= 0;
END //
DELIMITER ;

-- Trigger: Restore product stock when order items are deleted (cancellation)
DELIMITER //
CREATE TRIGGER restore_stock_on_order_delete
    AFTER DELETE ON order_items
    FOR EACH ROW
BEGIN
    UPDATE products 
    SET stock_quantity = stock_quantity + OLD.quantity,
        is_available = TRUE,
        updated_at = CURRENT_TIMESTAMP
    WHERE id = OLD.product_id;
END //
DELIMITER ;

-- Trigger: Validate order total calculation
DELIMITER //
CREATE TRIGGER validate_order_total_on_insert
    BEFORE INSERT ON orders
    FOR EACH ROW
BEGIN
    -- Ensure total amount is positive
    IF NEW.total_amount <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Order total must be positive';
    END IF;
    
    -- Set default order date if not provided
    IF NEW.order_date IS NULL THEN
        SET NEW.order_date = CURRENT_TIMESTAMP;
    END IF;
END //
DELIMITER ;

-- Trigger: Update user activity timestamp when orders are placed
DELIMITER //
CREATE TRIGGER update_user_activity_on_order
    AFTER INSERT ON orders
    FOR EACH ROW
BEGIN
    UPDATE users 
    SET updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.user_id;
END //
DELIMITER ;

-- Trigger: Validate review rating range
DELIMITER //
CREATE TRIGGER validate_review_rating
    BEFORE INSERT ON reviews
    FOR EACH ROW
BEGIN
    IF NEW.rating < 1 OR NEW.rating > 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rating must be between 1 and 5';
    END IF;
    
    -- Set review date if not provided
    IF NEW.review_date IS NULL THEN
        SET NEW.review_date = CURRENT_TIMESTAMP;
    END IF;
END //
DELIMITER ; 
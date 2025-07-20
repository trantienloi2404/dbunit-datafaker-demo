-- Sample data for DBUnit and DataFaker demo
SET FOREIGN_KEY_CHECKS=0;
-- Insert sample users
INSERT INTO users (username, email, first_name, last_name, date_of_birth, phone_number) VALUES
('john_doe', 'john.doe@example.com', 'John', 'Doe', '1990-05-15', '+1-555-0101'),
('jane_smith', 'jane.smith@example.com', 'Jane', 'Smith', '1985-08-22', '+1-555-0102'),
('bob_wilson', 'bob.wilson@example.com', 'Bob', 'Wilson', '1992-12-03', '+1-555-0103'),
('alice_brown', 'alice.brown@example.com', 'Alice', 'Brown', '1988-03-18', '+1-555-0104'),
('charlie_davis', 'charlie.davis@example.com', 'Charlie', 'Davis', '1995-07-09', '+1-555-0105');

-- Insert sample products
INSERT INTO products (name, description, price, category, sku, stock_quantity) VALUES
('Laptop Pro 15"', 'High-performance laptop with 16GB RAM and 512GB SSD', 1299.99, 'Electronics', 'LAP-PRO-15-001', 50),
('Wireless Mouse', 'Ergonomic wireless mouse with long battery life', 29.99, 'Electronics', 'MOU-WIR-001', 200),
('Coffee Mug', 'Ceramic coffee mug with heat-resistant handle', 12.99, 'Kitchen', 'MUG-CER-001', 150),
('Office Chair', 'Ergonomic office chair with lumbar support', 249.99, 'Furniture', 'CHA-OFF-001', 75),
('Bluetooth Headphones', 'Noise-cancelling wireless headphones', 199.99, 'Electronics', 'HEA-BLU-001', 100),
('Desk Lamp', 'LED desk lamp with adjustable brightness', 45.99, 'Furniture', 'LAM-DES-001', 80),
('Water Bottle', 'Stainless steel insulated water bottle 32oz', 24.99, 'Sports', 'BOT-WAT-001', 300),
('Running Shoes', 'Comfortable running shoes with cushioned sole', 89.99, 'Sports', 'SHO-RUN-001', 120);

-- Insert sample orders
INSERT INTO orders (user_id, order_number, total_amount, status, delivery_address) VALUES
(1, 'ORD-2024-001', 1329.98, 'DELIVERED', '123 Main St, New York, NY 10001'),
(2, 'ORD-2024-002', 42.98, 'SHIPPED', '456 Oak Ave, Los Angeles, CA 90210'),
(3, 'ORD-2024-003', 249.99, 'CONFIRMED', '789 Pine Rd, Chicago, IL 60601'),
(4, 'ORD-2024-004', 245.98, 'PENDING', '321 Elm St, Houston, TX 77001'),
(5, 'ORD-2024-005', 114.98, 'DELIVERED', '654 Maple Dr, Phoenix, AZ 85001');

-- Insert sample order items
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price) VALUES
-- Order 1 items
(1, 1, 1, 1299.99, 1299.99),
(1, 2, 1, 29.99, 29.99),
-- Order 2 items
(2, 3, 1, 12.99, 12.99),
(2, 2, 1, 29.99, 29.99),
-- Order 3 items
(3, 4, 1, 249.99, 249.99),
-- Order 4 items
(4, 5, 1, 199.99, 199.99),
(4, 6, 1, 45.99, 45.99),
-- Order 5 items
(5, 7, 1, 24.99, 24.99),
(5, 8, 1, 89.99, 89.99);

-- Insert sample reviews
INSERT INTO reviews (user_id, product_id, rating, title, comment, is_verified_purchase) VALUES
(1, 1, 5, 'Excellent laptop!', 'Fast performance and great build quality. Highly recommended!', TRUE),
(1, 2, 4, 'Good wireless mouse', 'Works well, comfortable to use. Battery lasts long.', TRUE),
(2, 3, 5, 'Perfect coffee mug', 'Great size and keeps coffee warm. Love the design!', TRUE),
(2, 2, 3, 'Decent mouse', 'Works fine but could be more responsive.', TRUE),
(3, 4, 5, 'Great office chair', 'Very comfortable for long work sessions. Worth the price!', TRUE),
(4, 5, 4, 'Good headphones', 'Sound quality is great, noise cancellation works well.', TRUE),
(4, 6, 4, 'Nice desk lamp', 'Bright LED light, adjustable. Good for reading.', TRUE),
(5, 7, 5, 'Best water bottle', 'Keeps drinks cold all day. Perfect for workouts!', TRUE),
(5, 8, 4, 'Comfortable shoes', 'Good for running, nice cushioning. Fits well.', TRUE);
SET FOREIGN_KEY_CHECKS=1;


-- docker exec -i dbunit-demo-mysql mysql -u testuser -ptestpass -e "DROP DATABASE IF EXISTS testdb;" && docker exec -i dbunit-demo-mysql mysql -u testuser -ptestpass -e "CREATE DATABASE testdb;" && docker exec -i dbunit-demo-mysql mysql -u testuser -ptestpass testdb < 01-create-schema.sql && docker exec -i dbunit-demo-mysql mysql -u testuser -ptestpass testdb < 02-sample-data.sql


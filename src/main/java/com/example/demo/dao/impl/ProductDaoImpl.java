package com.example.demo.dao.impl;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.demo.dao.ProductDao;
import com.example.demo.dto.ProductDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ProductDao interface.
 * Handles all database operations related to products.
 */
public class ProductDaoImpl implements ProductDao {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductDaoImpl.class);
    private final DatabaseConnectionManager connectionManager;
    
    public ProductDaoImpl() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    @Override
    public ProductDto create(ProductDto product) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, category, sku, stock_quantity, is_available) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setBigDecimal(3, product.getPrice());
            stmt.setString(4, product.getCategory());
            stmt.setString(5, product.getSku());
            stmt.setInt(6, product.getStockQuantity() != null ? product.getStockQuantity() : 0);
            stmt.setBoolean(7, product.getIsAvailable() != null ? product.getIsAvailable() : true);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating product failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    product.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating product failed, no ID obtained.");
                }
            }
            
            logger.info("Created product with ID: {}", product.getId());
            return product;
        }
    }
    
    @Override
    public Optional<ProductDto> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM products WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<ProductDto> findBySku(String sku) throws SQLException {
        String sql = "SELECT * FROM products WHERE sku = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, sku);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProduct(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<ProductDto> findByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM products WHERE category = ? ORDER BY created_at DESC";
        
        List<ProductDto> products = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, category);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        
        return products;
    }
    
    @Override
    public List<ProductDto> findAvailable() throws SQLException {
        String sql = "SELECT * FROM products WHERE is_available = TRUE ORDER BY created_at DESC";
        
        List<ProductDto> products = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        
        return products;
    }
    
    @Override
    public List<ProductDto> findWithStockAbove(int threshold) throws SQLException {
        String sql = "SELECT * FROM products WHERE stock_quantity > ? ORDER BY stock_quantity DESC";
        
        List<ProductDto> products = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, threshold);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        
        return products;
    }
    
    @Override
    public List<ProductDto> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) throws SQLException {
        String sql = "SELECT * FROM products WHERE price BETWEEN ? AND ? ORDER BY price ASC";
        
        List<ProductDto> products = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setBigDecimal(1, minPrice);
            stmt.setBigDecimal(2, maxPrice);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    products.add(mapResultSetToProduct(rs));
                }
            }
        }
        
        return products;
    }
    
    @Override
    public List<ProductDto> findAll() throws SQLException {
        String sql = "SELECT * FROM products ORDER BY created_at DESC";
        
        List<ProductDto> products = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                products.add(mapResultSetToProduct(rs));
            }
        }
        
        return products;
    }
    
    @Override
    public ProductDto update(ProductDto product) throws SQLException {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, category = ?, " +
                     "sku = ?, stock_quantity = ?, is_available = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getDescription());
            stmt.setBigDecimal(3, product.getPrice());
            stmt.setString(4, product.getCategory());
            stmt.setString(5, product.getSku());
            stmt.setInt(6, product.getStockQuantity());
            stmt.setBoolean(7, product.getIsAvailable());
            stmt.setLong(8, product.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating product failed, no rows affected.");
            }
            
            logger.info("Updated product with ID: {}", product.getId());
            return product;
        }
    }
    
    @Override
    public void updateStock(Long productId, int newQuantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, newQuantity);
            stmt.setLong(2, productId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating product stock failed, no rows affected.");
            }
            
            logger.info("Updated stock for product ID: {} to {}", productId, newQuantity);
        }
    }
    
    @Override
    public void reduceStock(Long productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setLong(2, productId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Reducing product stock failed, no rows affected.");
            }
            
            logger.info("Reduced stock for product ID: {} by {}", productId, quantity);
        }
    }
    
    @Override
    public void increaseStock(Long productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity + ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, quantity);
            stmt.setLong(2, productId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Increasing product stock failed, no rows affected.");
            }
            
            logger.info("Increased stock for product ID: {} by {}", productId, quantity);
        }
    }
    
    @Override
    public void markUnavailable(Long id) throws SQLException {
        String sql = "UPDATE products SET is_available = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Marking product unavailable failed, no rows affected.");
            }
            
            logger.info("Marked product ID: {} as unavailable", id);
        }
    }
    
    @Override
    public void markAvailable(Long id) throws SQLException {
        String sql = "UPDATE products SET is_available = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Marking product available failed, no rows affected.");
            }
            
            logger.info("Marked product ID: {} as available", id);
        }
    }
    
    @Override
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting product failed, no rows affected.");
            }
            
            logger.info("Deleted product with ID: {}", id);
        }
    }
    
    @Override
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM products";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }
    
    @Override
    public long countByCategory(String category) throws SQLException {
        String sql = "SELECT COUNT(*) FROM products WHERE category = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, category);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0;
    }
    
    private ProductDto mapResultSetToProduct(ResultSet rs) throws SQLException {
        ProductDto product = new ProductDto();
        product.setId(rs.getLong("id"));
        product.setName(rs.getString("name"));
        product.setDescription(rs.getString("description"));
        product.setPrice(rs.getBigDecimal("price"));
        product.setCategory(rs.getString("category"));
        product.setSku(rs.getString("sku"));
        product.setStockQuantity(rs.getInt("stock_quantity"));
        product.setIsAvailable(rs.getBoolean("is_available"));
        product.setCreatedAt(rs.getTimestamp("created_at"));
        product.setUpdatedAt(rs.getTimestamp("updated_at"));
        return product;
    }
} 
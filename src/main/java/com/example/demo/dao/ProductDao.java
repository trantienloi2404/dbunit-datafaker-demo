package com.example.demo.dao;

import com.example.demo.dto.ProductDto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Product entity.
 * Defines all database operations related to products.
 */
public interface ProductDao {
    
    /**
     * Creates a new product in the database.
     */
    ProductDto create(ProductDto product) throws SQLException;
    
    /**
     * Finds a product by ID.
     */
    Optional<ProductDto> findById(Long id) throws SQLException;
    
    /**
     * Finds a product by SKU.
     */
    Optional<ProductDto> findBySku(String sku) throws SQLException;
    
    /**
     * Finds products by category.
     */
    List<ProductDto> findByCategory(String category) throws SQLException;
    
    /**
     * Finds available products.
     */
    List<ProductDto> findAvailable() throws SQLException;
    
    /**
     * Finds products with stock above threshold.
     */
    List<ProductDto> findWithStockAbove(int threshold) throws SQLException;
    
    /**
     * Finds products in price range.
     */
    List<ProductDto> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) throws SQLException;
    
    /**
     * Finds all products.
     */
    List<ProductDto> findAll() throws SQLException;
    
    /**
     * Updates an existing product.
     */
    ProductDto update(ProductDto product) throws SQLException;
    
    /**
     * Updates product stock quantity.
     */
    void updateStock(Long productId, int newQuantity) throws SQLException;
    
    /**
     * Reduces product stock by quantity (for orders).
     */
    void reduceStock(Long productId, int quantity) throws SQLException;
    
    /**
     * Increases product stock by quantity (for returns/restocking).
     */
    void increaseStock(Long productId, int quantity) throws SQLException;
    
    /**
     * Marks product as unavailable.
     */
    void markUnavailable(Long id) throws SQLException;
    
    /**
     * Marks product as available.
     */
    void markAvailable(Long id) throws SQLException;
    
    /**
     * Deletes a product permanently.
     */
    void delete(Long id) throws SQLException;
    
    /**
     * Counts total number of products.
     */
    long count() throws SQLException;
    
    /**
     * Counts products by category.
     */
    long countByCategory(String category) throws SQLException;
} 
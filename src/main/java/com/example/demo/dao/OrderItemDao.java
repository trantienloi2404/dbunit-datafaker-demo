package com.example.demo.dao;

import com.example.demo.dto.OrderItemDto;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for OrderItem entity.
 * Defines all database operations related to order items.
 */
public interface OrderItemDao {
    
    /**
     * Creates a new order item in the database.
     */
    OrderItemDto create(OrderItemDto orderItem) throws SQLException;
    
    /**
     * Finds an order item by ID.
     */
    Optional<OrderItemDto> findById(Long id) throws SQLException;
    
    /**
     * Finds all order items for a specific order.
     */
    List<OrderItemDto> findByOrderId(Long orderId) throws SQLException;
    
    /**
     * Finds order items by product ID.
     */
    List<OrderItemDto> findByProductId(Long productId) throws SQLException;
    
    /**
     * Finds order items by order and product.
     */
    Optional<OrderItemDto> findByOrderIdAndProductId(Long orderId, Long productId) throws SQLException;
    
    /**
     * Finds all order items.
     */
    List<OrderItemDto> findAll() throws SQLException;
    
    /**
     * Updates an existing order item.
     */
    OrderItemDto update(OrderItemDto orderItem) throws SQLException;
    
    /**
     * Updates order item quantity.
     */
    void updateQuantity(Long orderItemId, int newQuantity) throws SQLException;
    
    /**
     * Deletes an order item permanently.
     */
    void delete(Long id) throws SQLException;
    
    /**
     * Deletes all order items for a specific order.
     */
    void deleteByOrderId(Long orderId) throws SQLException;
    
    /**
     * Counts total number of order items.
     */
    long count() throws SQLException;
    
    /**
     * Counts order items for a specific order.
     */
    long countByOrderId(Long orderId) throws SQLException;
} 
package com.example.demo.dao;

import com.example.demo.dto.OrderDto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Order entity.
 * Defines all database operations related to orders.
 */
public interface OrderDao {
    
    /**
     * Creates a new order in the database.
     */
    OrderDto create(OrderDto order) throws SQLException;
    
    /**
     * Finds an order by ID.
     */
    Optional<OrderDto> findById(Long id) throws SQLException;
    
    /**
     * Finds an order by order number.
     */
    Optional<OrderDto> findByOrderNumber(String orderNumber) throws SQLException;
    
    /**
     * Finds orders by user ID.
     */
    List<OrderDto> findByUserId(Long userId) throws SQLException;
    
    /**
     * Finds orders by status.
     */
    List<OrderDto> findByStatus(String status) throws SQLException;
    
    /**
     * Finds orders by user and status.
     */
    List<OrderDto> findByUserIdAndStatus(Long userId, String status) throws SQLException;
    
    /**
     * Finds orders within date range.
     */
    List<OrderDto> findByDateRange(Timestamp startDate, Timestamp endDate) throws SQLException;
    
    /**
     * Finds orders for sales report (DELIVERED/SHIPPED in last N days).
     */
    List<OrderDto> findForSalesReport(int daysBack) throws SQLException;
    
    /**
     * Finds all orders.
     */
    List<OrderDto> findAll() throws SQLException;
    
    /**
     * Updates an existing order.
     */
    OrderDto update(OrderDto order) throws SQLException;
    
    /**
     * Updates order status.
     */
    void updateStatus(Long orderId, String status) throws SQLException;
    
    /**
     * Updates shipped date.
     */
    void updateShippedDate(Long orderId, Timestamp shippedDate) throws SQLException;
    
    /**
     * Calculates total order value with tax.
     */
    BigDecimal calculateOrderTotal(Long orderId) throws SQLException;
    
    /**
     * Calculates total order value with custom tax rate.
     */
    BigDecimal calculateOrderTotalWithTax(Long orderId, BigDecimal taxRate) throws SQLException;
    
    /**
     * Gets user loyalty status based on order history.
     */
    String getUserLoyaltyStatus(Long userId) throws SQLException;
    
    /**
     * Deletes an order permanently.
     */
    void delete(Long id) throws SQLException;
    
    /**
     * Counts total number of orders.
     */
    long count() throws SQLException;
    
    /**
     * Counts orders by status.
     */
    long countByStatus(String status) throws SQLException;
    
    /**
     * Gets total revenue for completed orders.
     */
    BigDecimal getTotalRevenue() throws SQLException;
} 
package com.example.demo.dao.impl;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.demo.dao.OrderItemDao;
import com.example.demo.dto.OrderItemDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of OrderItemDao interface.
 * Handles all database operations related to order items.
 */
public class OrderItemDaoImpl implements OrderItemDao {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderItemDaoImpl.class);
    private final DatabaseConnectionManager connectionManager;
    
    public OrderItemDaoImpl() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    @Override
    public OrderItemDto create(OrderItemDto orderItem) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, orderItem.getOrderId());
            stmt.setLong(2, orderItem.getProductId());
            stmt.setInt(3, orderItem.getQuantity());
            stmt.setBigDecimal(4, orderItem.getUnitPrice());
            stmt.setBigDecimal(5, orderItem.getTotalPrice());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating order item failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    orderItem.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating order item failed, no ID obtained.");
                }
            }
            
            logger.info("Created order item with ID: {}", orderItem.getId());
            return orderItem;
        }
    }
    
    @Override
    public Optional<OrderItemDto> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOrderItem(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<OrderItemDto> findByOrderId(Long orderId) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE order_id = ? ORDER BY id";
        
        List<OrderItemDto> orderItems = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orderItems.add(mapResultSetToOrderItem(rs));
                }
            }
        }
        
        return orderItems;
    }
    
    @Override
    public List<OrderItemDto> findByProductId(Long productId) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE product_id = ? ORDER BY id DESC";
        
        List<OrderItemDto> orderItems = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orderItems.add(mapResultSetToOrderItem(rs));
                }
            }
        }
        
        return orderItems;
    }
    
    @Override
    public Optional<OrderItemDto> findByOrderIdAndProductId(Long orderId, Long productId) throws SQLException {
        String sql = "SELECT * FROM order_items WHERE order_id = ? AND product_id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            stmt.setLong(2, productId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOrderItem(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<OrderItemDto> findAll() throws SQLException {
        String sql = "SELECT * FROM order_items ORDER BY id DESC";
        
        List<OrderItemDto> orderItems = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                orderItems.add(mapResultSetToOrderItem(rs));
            }
        }
        
        return orderItems;
    }
    
    @Override
    public OrderItemDto update(OrderItemDto orderItem) throws SQLException {
        String sql = "UPDATE order_items SET order_id = ?, product_id = ?, quantity = ?, " +
                     "unit_price = ?, total_price = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, orderItem.getOrderId());
            stmt.setLong(2, orderItem.getProductId());
            stmt.setInt(3, orderItem.getQuantity());
            stmt.setBigDecimal(4, orderItem.getUnitPrice());
            stmt.setBigDecimal(5, orderItem.getTotalPrice());
            stmt.setLong(6, orderItem.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating order item failed, no rows affected.");
            }
            
            logger.info("Updated order item with ID: {}", orderItem.getId());
            return orderItem;
        }
    }
    
    @Override
    public void updateQuantity(Long orderItemId, int newQuantity) throws SQLException {
        String sql = "UPDATE order_items SET quantity = ?, total_price = unit_price * ? WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, newQuantity);
            stmt.setLong(3, orderItemId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating order item quantity failed, no rows affected.");
            }
            
            logger.info("Updated quantity for order item ID: {} to {}", orderItemId, newQuantity);
        }
    }
    
    @Override
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM order_items WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting order item failed, no rows affected.");
            }
            
            logger.info("Deleted order item with ID: {}", id);
        }
    }
    
    @Override
    public void deleteByOrderId(Long orderId) throws SQLException {
        String sql = "DELETE FROM order_items WHERE order_id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            
            int affectedRows = stmt.executeUpdate();
            logger.info("Deleted {} order items for order ID: {}", affectedRows, orderId);
        }
    }
    
    @Override
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM order_items";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }
    
    @Override
    public long countByOrderId(Long orderId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM order_items WHERE order_id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0;
    }
    
    private OrderItemDto mapResultSetToOrderItem(ResultSet rs) throws SQLException {
        OrderItemDto orderItem = new OrderItemDto();
        orderItem.setId(rs.getLong("id"));
        orderItem.setOrderId(rs.getLong("order_id"));
        orderItem.setProductId(rs.getLong("product_id"));
        orderItem.setQuantity(rs.getInt("quantity"));
        orderItem.setUnitPrice(rs.getBigDecimal("unit_price"));
        orderItem.setTotalPrice(rs.getBigDecimal("total_price"));
        return orderItem;
    }
} 
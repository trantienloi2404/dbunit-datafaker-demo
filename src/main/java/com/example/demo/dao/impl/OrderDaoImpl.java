package com.example.demo.dao.impl;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.demo.dao.OrderDao;
import com.example.demo.dto.OrderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of OrderDao interface.
 * Handles all database operations related to orders.
 */
public class OrderDaoImpl implements OrderDao {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderDaoImpl.class);
    private final DatabaseConnectionManager connectionManager;
    
    public OrderDaoImpl() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    @Override
    public OrderDto create(OrderDto order) throws SQLException {
        String sql = "INSERT INTO orders (user_id, order_number, total_amount, status, delivery_address) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, order.getUserId());
            stmt.setString(2, order.getOrderNumber());
            stmt.setBigDecimal(3, order.getTotalAmount());
            stmt.setString(4, order.getStatus() != null ? order.getStatus() : "PENDING");
            stmt.setString(5, order.getDeliveryAddress());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating order failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    order.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating order failed, no ID obtained.");
                }
            }
            
            logger.info("Created order with ID: {}", order.getId());
            return order;
        }
    }
    
    @Override
    public Optional<OrderDto> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM orders WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOrder(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<OrderDto> findByOrderNumber(String orderNumber) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_number = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, orderNumber);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToOrder(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<OrderDto> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id = ? ORDER BY order_date DESC";
        
        List<OrderDto> orders = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        
        return orders;
    }
    
    @Override
    public List<OrderDto> findByStatus(String status) throws SQLException {
        String sql = "SELECT * FROM orders WHERE status = ? ORDER BY order_date DESC";
        
        List<OrderDto> orders = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        
        return orders;
    }
    
    @Override
    public List<OrderDto> findByUserIdAndStatus(Long userId, String status) throws SQLException {
        String sql = "SELECT * FROM orders WHERE user_id = ? AND status = ? ORDER BY order_date DESC";
        
        List<OrderDto> orders = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, status);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        
        return orders;
    }
    
    @Override
    public List<OrderDto> findByDateRange(Timestamp startDate, Timestamp endDate) throws SQLException {
        String sql = "SELECT * FROM orders WHERE order_date BETWEEN ? AND ? ORDER BY order_date DESC";
        
        List<OrderDto> orders = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setTimestamp(1, startDate);
            stmt.setTimestamp(2, endDate);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        
        return orders;
    }
    
    @Override
    public List<OrderDto> findForSalesReport(int daysBack) throws SQLException {
        String sql = "SELECT * FROM orders WHERE status IN ('DELIVERED', 'SHIPPED') " +
                     "AND order_date >= DATE_SUB(CURRENT_DATE, INTERVAL ? DAY) " +
                     "ORDER BY order_date DESC";
        
        List<OrderDto> orders = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, daysBack);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    orders.add(mapResultSetToOrder(rs));
                }
            }
        }
        
        return orders;
    }
    
    @Override
    public List<OrderDto> findAll() throws SQLException {
        String sql = "SELECT * FROM orders ORDER BY order_date DESC";
        
        List<OrderDto> orders = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                orders.add(mapResultSetToOrder(rs));
            }
        }
        
        return orders;
    }
    
    @Override
    public OrderDto update(OrderDto order) throws SQLException {
        String sql = "UPDATE orders SET user_id = ?, order_number = ?, total_amount = ?, status = ?, " +
                     "shipped_date = ?, delivery_address = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, order.getUserId());
            stmt.setString(2, order.getOrderNumber());
            stmt.setBigDecimal(3, order.getTotalAmount());
            stmt.setString(4, order.getStatus());
            stmt.setTimestamp(5, order.getShippedDate());
            stmt.setString(6, order.getDeliveryAddress());
            stmt.setLong(7, order.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating order failed, no rows affected.");
            }
            
            logger.info("Updated order with ID: {}", order.getId());
            return order;
        }
    }
    
    @Override
    public void updateStatus(Long orderId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setLong(2, orderId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating order status failed, no rows affected.");
            }
            
            logger.info("Updated order status for ID: {} to {}", orderId, status);
        }
    }
    
    @Override
    public void updateShippedDate(Long orderId, Timestamp shippedDate) throws SQLException {
        String sql = "UPDATE orders SET shipped_date = ? WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setTimestamp(1, shippedDate);
            stmt.setLong(2, orderId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating shipped date failed, no rows affected.");
            }
            
            logger.info("Updated shipped date for order ID: {}", orderId);
        }
    }
    
    @Override
    public BigDecimal calculateOrderTotal(Long orderId) throws SQLException {
        String sql = "SELECT calculate_order_total(?) as total_with_tax";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total_with_tax");
                }
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal calculateOrderTotalWithTax(Long orderId, BigDecimal taxRate) throws SQLException {
        String sql = "SELECT calculate_order_total_with_tax(?, ?) as total_with_custom_tax";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, orderId);
            stmt.setBigDecimal(2, taxRate);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("total_with_custom_tax");
                }
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    @Override
    public String getUserLoyaltyStatus(Long userId) throws SQLException {
        String sql = "SELECT get_user_loyalty_status(?) as loyalty_status";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("loyalty_status");
                }
            }
        }
        
        return "BRONZE";
    }
    
    @Override
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM orders WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting order failed, no rows affected.");
            }
            
            logger.info("Deleted order with ID: {}", id);
        }
    }
    
    @Override
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }
    
    @Override
    public long countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM orders WHERE status = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, status);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return 0;
    }
    
    @Override
    public BigDecimal getTotalRevenue() throws SQLException {
        String sql = "SELECT SUM(total_amount) as total_revenue FROM orders WHERE status IN ('DELIVERED', 'SHIPPED')";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                BigDecimal revenue = rs.getBigDecimal("total_revenue");
                return revenue != null ? revenue : BigDecimal.ZERO;
            }
        }
        
        return BigDecimal.ZERO;
    }
    
    private OrderDto mapResultSetToOrder(ResultSet rs) throws SQLException {
        OrderDto order = new OrderDto();
        order.setId(rs.getLong("id"));
        order.setUserId(rs.getLong("user_id"));
        order.setOrderNumber(rs.getString("order_number"));
        order.setTotalAmount(rs.getBigDecimal("total_amount"));
        order.setStatus(rs.getString("status"));
        order.setOrderDate(rs.getTimestamp("order_date"));
        order.setShippedDate(rs.getTimestamp("shipped_date"));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        return order;
    }
} 
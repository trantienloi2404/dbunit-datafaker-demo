package com.example.demo.dao.impl;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.demo.dao.UserDao;
import com.example.demo.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of UserDao interface.
 * Handles all database operations related to users.
 */
public class UserDaoImpl implements UserDao {
    
    private static final Logger logger = LoggerFactory.getLogger(UserDaoImpl.class);
    private final DatabaseConnectionManager connectionManager;
    
    public UserDaoImpl() {
        this.connectionManager = DatabaseConnectionManager.getInstance();
    }
    
    @Override
    public UserDto create(UserDto user) throws SQLException {
        String sql = "INSERT INTO users (username, email, first_name, last_name, date_of_birth, phone_number, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getFirstName());
            stmt.setString(4, user.getLastName());
            stmt.setDate(5, user.getDateOfBirth());
            stmt.setString(6, user.getPhoneNumber());
            stmt.setBoolean(7, user.getIsActive() != null ? user.getIsActive() : true);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }
            
            logger.info("Created user with ID: {}", user.getId());
            return user;
        }
    }
    
    @Override
    public Optional<UserDto> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<UserDto> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, username);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<UserDto> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public Optional<UserDto> findByUsernameOrEmail(String usernameOrEmail) throws SQLException {
        String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND is_active = TRUE";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, usernameOrEmail);
            stmt.setString(2, usernameOrEmail);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    @Override
    public List<UserDto> findAllActive() throws SQLException {
        String sql = "SELECT * FROM users WHERE is_active = TRUE ORDER BY created_at DESC";
        
        List<UserDto> users = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        
        return users;
    }
    
    @Override
    public List<UserDto> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        
        List<UserDto> users = new ArrayList<>();
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        
        return users;
    }
    
    @Override
    public UserDto update(UserDto user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ?, first_name = ?, last_name = ?, " +
                     "date_of_birth = ?, phone_number = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getFirstName());
            stmt.setString(4, user.getLastName());
            stmt.setDate(5, user.getDateOfBirth());
            stmt.setString(6, user.getPhoneNumber());
            stmt.setBoolean(7, user.getIsActive());
            stmt.setLong(8, user.getId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating user failed, no rows affected.");
            }
            
            logger.info("Updated user with ID: {}", user.getId());
            return user;
        }
    }
    
    @Override
    public void deactivate(Long id) throws SQLException {
        String sql = "UPDATE users SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deactivating user failed, no rows affected.");
            }
            
            logger.info("Deactivated user with ID: {}", id);
        }
    }
    
    @Override
    public void activate(Long id) throws SQLException {
        String sql = "UPDATE users SET is_active = TRUE, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Activating user failed, no rows affected.");
            }
            
            logger.info("Activated user with ID: {}", id);
        }
    }
    
    @Override
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql)) {
            stmt.setLong(1, id);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting user failed, no rows affected.");
            }
            
            logger.info("Deleted user with ID: {}", id);
        }
    }
    
    @Override
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }
    
    @Override
    public long countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = TRUE";
        
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        
        return 0;
    }
    
    private UserDto mapResultSetToUser(ResultSet rs) throws SQLException {
        UserDto user = new UserDto();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setDateOfBirth(rs.getDate("date_of_birth"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setCreatedAt(rs.getTimestamp("created_at"));
        user.setUpdatedAt(rs.getTimestamp("updated_at"));
        user.setIsActive(rs.getBoolean("is_active"));
        return user;
    }
} 
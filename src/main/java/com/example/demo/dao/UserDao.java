package com.example.demo.dao;

import com.example.demo.dto.UserDto;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for User entity.
 * Defines all database operations related to users.
 */
public interface UserDao {
    
    /**
     * Creates a new user in the database.
     */
    UserDto create(UserDto user) throws SQLException;
    
    /**
     * Finds a user by ID.
     */
    Optional<UserDto> findById(Long id) throws SQLException;
    
    /**
     * Finds a user by username.
     */
    Optional<UserDto> findByUsername(String username) throws SQLException;
    
    /**
     * Finds a user by email.
     */
    Optional<UserDto> findByEmail(String email) throws SQLException;
    
    /**
     * Finds users by username or email (for authentication).
     */
    Optional<UserDto> findByUsernameOrEmail(String usernameOrEmail) throws SQLException;
    
    /**
     * Finds all active users.
     */
    List<UserDto> findAllActive() throws SQLException;
    
    /**
     * Finds all users.
     */
    List<UserDto> findAll() throws SQLException;
    
    /**
     * Updates an existing user.
     */
    UserDto update(UserDto user) throws SQLException;
    
    /**
     * Deactivates a user (soft delete).
     */
    void deactivate(Long id) throws SQLException;
    
    /**
     * Activates a user.
     */
    void activate(Long id) throws SQLException;
    
    /**
     * Deletes a user permanently.
     */
    void delete(Long id) throws SQLException;
    
    /**
     * Counts total number of users.
     */
    long count() throws SQLException;
    
    /**
     * Counts active users.
     */
    long countActive() throws SQLException;
} 
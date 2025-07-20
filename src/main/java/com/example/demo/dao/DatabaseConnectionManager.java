package com.example.demo.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Database connection manager for DAO layer operations.
 * Provides connection management and transaction control.
 */
public class DatabaseConnectionManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionManager.class);
    
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";
    private static final String DB_USER = "testuser";
    private static final String DB_PASSWORD = "testpass";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    private static DatabaseConnectionManager instance;
    private ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();
    
    private DatabaseConnectionManager() {
        try {
            Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }
    }
    
    /**
     * Gets the singleton instance of the connection manager.
     */
    public static synchronized DatabaseConnectionManager getInstance() {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }
    
    /**
     * Gets a connection for the current thread.
     * Creates a new connection if one doesn't exist.
     */
    public Connection getConnection() throws SQLException {
        Connection connection = connectionHolder.get();
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false); // Enable transaction control
            connectionHolder.set(connection);
            logger.debug("Created new database connection for thread: {}", Thread.currentThread().getName());
        }
        return connection;
    }
    
    /**
     * Commits the current transaction for the thread.
     */
    public void commit() throws SQLException {
        Connection connection = connectionHolder.get();
        if (connection != null && !connection.isClosed()) {
            connection.commit();
            logger.debug("Transaction committed for thread: {}", Thread.currentThread().getName());
        }
    }
    
    /**
     * Rolls back the current transaction for the thread.
     */
    public void rollback() throws SQLException {
        Connection connection = connectionHolder.get();
        if (connection != null && !connection.isClosed()) {
            connection.rollback();
            logger.debug("Transaction rolled back for thread: {}", Thread.currentThread().getName());
        }
    }
    
    /**
     * Closes the connection for the current thread.
     */
    public void closeConnection() {
        Connection connection = connectionHolder.get();
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                    logger.debug("Database connection closed for thread: {}", Thread.currentThread().getName());
                }
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            } finally {
                connectionHolder.remove();
            }
        }
    }
    
    /**
     * Executes code within a transaction, automatically committing or rolling back.
     */
    public void executeInTransaction(TransactionCallback callback) throws SQLException {
        try {
            callback.execute();
            commit();
        } catch (Exception e) {
            rollback();
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Transaction failed", e);
            }
        }
    }
    
    /**
     * Callback interface for transaction execution.
     */
    public interface TransactionCallback {
        void execute() throws SQLException;
    }
} 
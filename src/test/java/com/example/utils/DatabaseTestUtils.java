package com.example.utils;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.utils.TestDataGenerator.TestDataSummary;
import com.example.demo.dao.DaoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Utility class for database testing with the DAO layer.
 * Provides methods for database connection management and data operations through DAOs.
 */
public class DatabaseTestUtils {
    
    // Logger for information
    private static final Logger logger = LoggerFactory.getLogger(DatabaseTestUtils.class);
    
    /**
     * Returns the database connection manager instance.
     */
    public static DatabaseConnectionManager getConnectionManager() {
        return DatabaseConnectionManager.getInstance();
    }
    
    /**
     * Returns the DAO factory instance.
     */
    public static DaoFactory getDaoFactory() {
        return DaoFactory.getInstance();
    }
    
    /**
     * Creates a test data generator with a fixed seed for reproducibility.
     */
    public static TestDataGenerator createTestDataGenerator() {
        return new TestDataGenerator(12345L);
    }
    
    /**
     * Creates a test data generator with a custom seed.
     */
    public static TestDataGenerator createTestDataGenerator(long seed) {
        return new TestDataGenerator(seed);
    }
    
    /**
     * Sets up test data using DAOs.
     */
    public static TestDataGenerator.TestDataSummary setupTestData(int userCount, int productCount, int orderCount) throws SQLException {
        TestDataGenerator generator = createTestDataGenerator();
        return generator.generateCompleteTestData(userCount, productCount, orderCount);
    }
    
    /**
     * Cleans up test data using DAOs.
     */
    public static void cleanupTestData(TestDataGenerator.TestDataSummary summary) throws SQLException {
        TestDataGenerator generator = createTestDataGenerator();
        generator.cleanupTestData(summary);
    }
    
    /**
     * Commits the current transaction.
     */
    public static void commit() throws SQLException {
        getConnectionManager().commit();
    }
    
    /**
     * Rolls back the current transaction.
     */
    public static void rollback() throws SQLException {
        getConnectionManager().rollback();
    }
    
    /**
     * Closes the database connection for the current thread.
     */
    public static void closeConnection() {
        getConnectionManager().closeConnection();
    }
    
    /**
     * Executes code within a transaction using the DAO layer.
     */
    public static void executeInTransaction(DatabaseConnectionManager.TransactionCallback callback) throws SQLException {
        getConnectionManager().executeInTransaction(callback);
    }
} 
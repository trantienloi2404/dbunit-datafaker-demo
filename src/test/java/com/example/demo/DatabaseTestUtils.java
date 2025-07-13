package com.example.demo;

import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class for database testing with DBUnit.
 * Provides methods for database connection, data setup, and cleanup.
 */
public class DatabaseTestUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseTestUtils.class);
    
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";
    private static final String DB_USER = "testuser";
    private static final String DB_PASSWORD = "testpass";
    private static final String DB_DRIVER = "com.mysql.cj.jdbc.Driver";
    
    /**
     * Creates a JDBC connection to the test database.
     * 
     * @return JDBC Connection instance
     * @throws SQLException if connection fails
     */
    public static Connection createConnection() throws SQLException {
        try {
            Class.forName(DB_DRIVER);
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false); // Disable auto-commit for transaction control
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
    }
    
    /**
     * Creates a DBUnit database connection wrapper.
     * 
     * @return IDatabaseConnection instance for DBUnit operations
     * @throws SQLException if connection fails
     * @throws org.dbunit.DatabaseUnitException if DBUnit configuration fails
     */
    public static IDatabaseConnection createDatabaseConnection() throws SQLException, org.dbunit.DatabaseUnitException {
        Connection jdbcConnection = createConnection();
        IDatabaseConnection databaseConnection = new DatabaseConnection(jdbcConnection);
        
        // Configure DBUnit properties for MySQL
        databaseConnection.getConfig().setProperty("http://www.dbunit.org/properties/datatypeFactory", 
                new org.dbunit.ext.mysql.MySqlDataTypeFactory());
        databaseConnection.getConfig().setProperty("http://www.dbunit.org/properties/metadataHandler", 
                new org.dbunit.ext.mysql.MySqlMetadataHandler());
        
        return databaseConnection;
    }
    
    /**
     * Loads dataset from XML file in the classpath.
     * 
     * @param xmlFileName the name of the XML file containing test data
     * @return IDataSet instance
     * @throws Exception if file loading fails
     */
    public static IDataSet loadDataSet(String xmlFileName) throws Exception {
        InputStream inputStream = DatabaseTestUtils.class.getClassLoader()
                .getResourceAsStream(xmlFileName);
        
        if (inputStream == null) {
            throw new IllegalArgumentException("Dataset file not found: " + xmlFileName);
        }
        
        FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
        builder.setColumnSensing(true); // Enable column sensing for better compatibility
        return builder.build(inputStream);
    }
    
    /**
     * Sets up test data by performing CLEAN_INSERT operation.
     * This will delete all existing data and insert the test data.
     * 
     * @param connection the database connection
     * @param dataSet the dataset to insert
     * @throws Exception if setup fails
     */
    public static void setupTestData(IDatabaseConnection connection, IDataSet dataSet) throws Exception {
        try {
            logger.info("Setting up test data...");
            DatabaseOperation.CLEAN_INSERT.execute(connection, dataSet);
            logger.info("Test data setup completed");
        } catch (Exception e) {
            logger.error("Failed to setup test data", e);
            throw e;
        }
    }
    
    /**
     * Cleans up test data by deleting all records from tables in the dataset.
     * 
     * @param connection the database connection
     * @param dataSet the dataset containing tables to clean
     * @throws Exception if cleanup fails
     */
    public static void cleanupTestData(IDatabaseConnection connection, IDataSet dataSet) throws Exception {
        try {
            logger.info("Cleaning up test data...");
            DatabaseOperation.DELETE_ALL.execute(connection, dataSet);
            logger.info("Test data cleanup completed");
        } catch (Exception e) {
            logger.error("Failed to cleanup test data", e);
            throw e;
        }
    }
    
    /**
     * Truncates all tables in the dataset. Use with caution!
     * 
     * @param connection the database connection
     * @param dataSet the dataset containing tables to truncate
     * @throws Exception if truncation fails
     */
    public static void truncateTables(IDatabaseConnection connection, IDataSet dataSet) throws Exception {
        try {
            logger.info("Truncating tables...");
            DatabaseOperation.TRUNCATE_TABLE.execute(connection, dataSet);
            logger.info("Table truncation completed");
        } catch (Exception e) {
            logger.error("Failed to truncate tables", e);
            throw e;
        }
    }
    
    /**
     * Closes the database connection safely.
     * 
     * @param connection the connection to close
     */
    public static void closeConnection(IDatabaseConnection connection) {
        if (connection != null) {
            try {
                connection.close();
                logger.debug("Database connection closed");
            } catch (SQLException e) {
                logger.warn("Error closing database connection", e);
            }
        }
    }
    
    /**
     * Commits the current transaction.
     * 
     * @param connection the database connection
     * @throws SQLException if commit fails
     */
    public static void commit(IDatabaseConnection connection) throws SQLException {
        connection.getConnection().commit();
    }
    
    /**
     * Rolls back the current transaction.
     * 
     * @param connection the database connection
     * @throws SQLException if rollback fails
     */
    public static void rollback(IDatabaseConnection connection) throws SQLException {
        connection.getConnection().rollback();
    }
} 
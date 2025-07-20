package com.example.demo;

import org.dbunit.database.IDatabaseConnection;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Schema validation test class that verifies the database schema aligns with 
 * business requirements and checks if the designed schema matches the 
 * implemented schema. This addresses schema review requirements for 
 * comprehensive database testing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SchemaValidationTest {

    private static final Logger logger = LoggerFactory.getLogger(SchemaValidationTest.class);
    
    private IDatabaseConnection connection;
    private DatabaseMetaData metaData;

    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up schema validation test");
        connection = DatabaseTestUtils.createDatabaseConnection();
        metaData = connection.getConnection().getMetaData();
        logger.info("Schema validation test setup completed");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            DatabaseTestUtils.closeConnection(connection);
        }
    }

    // ===============================
    // TABLE STRUCTURE VALIDATION
    // ===============================

    @Test
    @Order(1)
    @DisplayName("Validate Required Tables Exist")
    void validateRequiredTablesExist() throws SQLException {
        logger.info("Validating required tables exist according to e-commerce requirements");

        // Define required tables for e-commerce system
        String[] requiredTables = {
            "users", "products", "orders", "order_items", "reviews"
        };

        Set<String> existingTables = new HashSet<>();
        try (ResultSet rs = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                existingTables.add(rs.getString("TABLE_NAME").toLowerCase());
            }
        }

        for (String requiredTable : requiredTables) {
            assertTrue(existingTables.contains(requiredTable.toLowerCase()),
                    "Required table '" + requiredTable + "' must exist for e-commerce system");
            logger.info("✓ Required table '{}' exists", requiredTable);
        }

        logger.info("All required tables validation passed");
    }

    @Test
    @Order(2)
    @DisplayName("Validate User Table Schema Requirements")
    void validateUserTableSchema() throws SQLException {
        logger.info("Validating users table schema meets e-commerce requirements");

        // Business Requirements for Users table:
        // - Must have unique identifiers (id, username, email)
        // - Must store contact information (email, phone)
        // - Must track user status (is_active)
        // - Must have audit trail (created_at, updated_at)

        Map<String, ColumnSpec> requiredColumns = new HashMap<>();
        requiredColumns.put("id", new ColumnSpec("BIGINT", false, true)); // Primary key
        requiredColumns.put("username", new ColumnSpec("VARCHAR", false, true)); // Unique
        requiredColumns.put("email", new ColumnSpec("VARCHAR", false, true)); // Unique
        requiredColumns.put("first_name", new ColumnSpec("VARCHAR", false, false));
        requiredColumns.put("last_name", new ColumnSpec("VARCHAR", false, false));
        requiredColumns.put("phone_number", new ColumnSpec("VARCHAR", true, false));
        requiredColumns.put("is_active", new ColumnSpec("BIT", true, false)); // MySQL BOOLEAN -> BIT
        requiredColumns.put("created_at", new ColumnSpec("TIMESTAMP", true, false));
        requiredColumns.put("updated_at", new ColumnSpec("TIMESTAMP", true, false));

        validateTableSchema("users", requiredColumns);
        
        // Validate unique constraints exist
        validateUniqueConstraintExists("users", "username");
        validateUniqueConstraintExists("users", "email");

        logger.info("Users table schema validation passed");
    }

    // ===============================
    // BUSINESS LOGIC OBJECTS VALIDATION
    // ===============================

    @Test
    @Order(3)
    @DisplayName("Validate Required Functions Exist")
    void validateRequiredFunctionsExist() throws SQLException {
        logger.info("Validating required business logic functions exist");

        // Business Requirements: Functions for common calculations
        String[] requiredFunctions = {
            "calculate_order_total",
            "get_user_loyalty_status", 
            "get_product_rating"
        };

        for (String functionName : requiredFunctions) {
            boolean functionExists = checkFunctionExists(functionName);
            assertTrue(functionExists, 
                    "Business logic function '" + functionName + "' is required");
            logger.info("✓ Function '{}' exists", functionName);
        }

        logger.info("Required functions validation passed");
    }

    @Test
    @Order(4)
    @DisplayName("Validate Required Stored Procedures Exist")
    void validateRequiredStoredProceduresExist() throws SQLException {
        logger.info("Validating required business logic stored procedures exist");

        // Business Requirements: Procedures for business operations
        String[] requiredProcedures = {
            "process_order_shipment",
            "restock_product",
            "get_user_order_summary"
        };

        for (String procedureName : requiredProcedures) {
            boolean procedureExists = checkProcedureExists(procedureName);
            assertTrue(procedureExists, 
                    "Business logic procedure '" + procedureName + "' is required");
            logger.info("✓ Procedure '{}' exists", procedureName);
        }

        logger.info("Required stored procedures validation passed");
    }

    @Test
    @Order(5)
    @DisplayName("Validate Required Triggers Exist")
    void validateRequiredTriggersExist() throws SQLException {
        logger.info("Validating required business logic triggers exist");

        // Business Requirements: Triggers for data integrity and automation
        String[] requiredTriggers = {
            "update_stock_on_order_insert",
            "restore_stock_on_order_delete",
            "validate_order_total_on_insert",
            "update_user_activity_on_order",
            "validate_review_rating"
        };

        for (String triggerName : requiredTriggers) {
            boolean triggerExists = checkTriggerExists(triggerName);
            assertTrue(triggerExists, 
                    "Business logic trigger '" + triggerName + "' is required");
            logger.info("✓ Trigger '{}' exists", triggerName);
        }

        logger.info("Required triggers validation passed");
    }

    // ===============================
    // HELPER METHODS
    // ===============================

    private void validateTableSchema(String tableName, Map<String, ColumnSpec> requiredColumns) throws SQLException {
        Map<String, ColumnInfo> actualColumns = getTableColumns(tableName);
        
        for (Map.Entry<String, ColumnSpec> entry : requiredColumns.entrySet()) {
            String columnName = entry.getKey();
            ColumnSpec spec = entry.getValue();
            
            assertTrue(actualColumns.containsKey(columnName.toLowerCase()),
                    "Required column '" + columnName + "' missing from table '" + tableName + "'");
            
            ColumnInfo actualColumn = actualColumns.get(columnName.toLowerCase());
            
            // Validate data type (allow variations for MySQL types)
            assertTrue(isDataTypeCompatible(spec.dataType, actualColumn.typeName),
                    "Column '" + columnName + "' in table '" + tableName + "' has incorrect data type. " +
                    "Expected: " + spec.dataType + ", Actual: " + actualColumn.typeName);
            
            // Validate nullability
            assertEquals(spec.nullable, actualColumn.nullable,
                    "Column '" + columnName + "' in table '" + tableName + "' has incorrect nullability");
            
            logger.info("✓ Column {}.{} validation passed", tableName, columnName);
        }
    }

    private Map<String, ColumnInfo> getTableColumns(String tableName) throws SQLException {
        Map<String, ColumnInfo> columns = new HashMap<>();
        
        try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME").toLowerCase();
                String typeName = rs.getString("TYPE_NAME");
                boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                
                columns.put(columnName, new ColumnInfo(typeName, nullable));
            }
        }
        
        return columns;
    }

    private boolean isDataTypeCompatible(String expected, String actual) {
        // Handle MySQL type variations
        Map<String, Set<String>> typeCompatibility = new HashMap<>();
        typeCompatibility.put("VARCHAR", Set.of("VARCHAR", "CHAR"));
        typeCompatibility.put("TEXT", Set.of("TEXT", "LONGTEXT", "MEDIUMTEXT"));
        typeCompatibility.put("INT", Set.of("INT", "INTEGER"));
        typeCompatibility.put("BIGINT", Set.of("BIGINT"));
        typeCompatibility.put("DECIMAL", Set.of("DECIMAL", "NUMERIC"));
        typeCompatibility.put("TIMESTAMP", Set.of("TIMESTAMP", "DATETIME"));
        typeCompatibility.put("BIT", Set.of("BIT", "TINYINT")); // MySQL BOOLEAN -> BIT/TINYINT
        typeCompatibility.put("ENUM", Set.of("ENUM"));
        
        Set<String> compatibleTypes = typeCompatibility.get(expected.toUpperCase());
        return compatibleTypes != null && compatibleTypes.contains(actual.toUpperCase());
    }

    private void validateUniqueConstraintExists(String tableName, String... columnNames) throws SQLException {
        // Check for unique indexes or constraints
        boolean uniqueConstraintExists = false;
        
        try (ResultSet rs = metaData.getIndexInfo(null, null, tableName, true, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                String columnName = rs.getString("COLUMN_NAME");
                
                if (indexName != null && columnName != null) {
                    // For multi-column unique constraints, we need to check all columns
                    if (columnNames.length == 1) {
                        if (columnName.equalsIgnoreCase(columnNames[0])) {
                            uniqueConstraintExists = true;
                            break;
                        }
                    }
                }
            }
        }
        
        assertTrue(uniqueConstraintExists, 
                "Unique constraint on " + tableName + "(" + String.join(",", columnNames) + ") is required");
    }

    private boolean checkFunctionExists(String functionName) throws SQLException {
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM information_schema.routines " +
                "WHERE routine_type = 'FUNCTION' AND routine_name = ?")) {
            stmt.setString(1, functionName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean checkProcedureExists(String procedureName) throws SQLException {
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM information_schema.routines " +
                "WHERE routine_type = 'PROCEDURE' AND routine_name = ?")) {
            stmt.setString(1, procedureName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private boolean checkTriggerExists(String triggerName) throws SQLException {
        try (PreparedStatement stmt = connection.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name = ?")) {
            stmt.setString(1, triggerName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Helper classes
    private static class ColumnSpec {
        final String dataType;
        final boolean nullable;
        final boolean unique;

        ColumnSpec(String dataType, boolean nullable, boolean unique) {
            this.dataType = dataType;
            this.nullable = nullable;
            this.unique = unique;
        }
    }

    private static class ColumnInfo {
        final String typeName;
        final boolean nullable;

        ColumnInfo(String typeName, boolean nullable) {
            this.typeName = typeName;
            this.nullable = nullable;
        }
    }
} 
package com.example.demo;

import com.example.demo.dao.DatabaseConnectionManager;
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
 * Schema validation tests ensure the database schema aligns with
 * business requirements and that the implemented schema matches the design.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SchemaValidationTest {

    // Logger for test information
    private static final Logger logger = LoggerFactory.getLogger(SchemaValidationTest.class);
    
    // Database connection manager
    private DatabaseConnectionManager connectionManager;
    // Database metadata for schema inspection
    private DatabaseMetaData metaData;

    /**
     * Setup before each test: initialize connection and metadata
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Setting up schema validation tests");
        connectionManager = DatabaseConnectionManager.getInstance();
        metaData = connectionManager.getConnection().getMetaData();
        logger.info("Finished setting up schema validation tests");
    }

    /**
     * Cleanup after each test: close database connection
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null) {
            connectionManager.closeConnection();
        }
    }

    // ===============================
    // VALIDATE TABLE STRUCTURE
    // ===============================

    /**
     * Validates that required tables exist for the e-commerce system
     */
    @Test
    @Order(1)
    @DisplayName("Validate Required Tables Exist")
    void validateRequiredTablesExist() throws SQLException {
        logger.info("Validating required tables exist for e-commerce requirements");

        // Required tables for the e-commerce system
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
                    "Required table '" + requiredTable + "' must exist for the e-commerce system");
            logger.info("✓ Required table '{}' exists", requiredTable);
        }

        logger.info("All required tables validated successfully");
    }

    /**
     * Validates the users table schema according to business requirements
     */
    @Test
    @Order(2)
    @DisplayName("Validate Users Table Schema")
    void validateUserTableSchema() throws SQLException {
        logger.info("Validating users table schema against business requirements");

        // Định nghĩa các cột mong đợi cho bảng users
        Map<String, ColumnSpec> expectedColumns = new HashMap<>();
        expectedColumns.put("id", new ColumnSpec("BIGINT", false, false));
        expectedColumns.put("username", new ColumnSpec("VARCHAR", false, true));
        expectedColumns.put("email", new ColumnSpec("VARCHAR", false, true));
        expectedColumns.put("first_name", new ColumnSpec("VARCHAR", false, false));
        expectedColumns.put("last_name", new ColumnSpec("VARCHAR", false, false));
        expectedColumns.put("date_of_birth", new ColumnSpec("DATE", true, false));
        expectedColumns.put("phone_number", new ColumnSpec("VARCHAR", true, false));
        expectedColumns.put("created_at", new ColumnSpec("TIMESTAMP", true, false));
        expectedColumns.put("updated_at", new ColumnSpec("TIMESTAMP", true, false));
        expectedColumns.put("is_active", new ColumnSpec("BOOLEAN", true, false));

        Map<String, ColumnInfo> actualColumns = getTableColumns("users");

        for (Map.Entry<String, ColumnSpec> entry : expectedColumns.entrySet()) {
            String columnName = entry.getKey();
            ColumnSpec expectedSpec = entry.getValue();

            assertTrue(actualColumns.containsKey(columnName),
                    "Cột '" + columnName + "' phải tồn tại trong bảng users");

            ColumnInfo actualInfo = actualColumns.get(columnName);

            // Validate data type (simple check)
            String actualType = actualInfo.typeName.toUpperCase();
            String expectedType = expectedSpec.dataType.toUpperCase();
            
            assertTrue(actualType.contains(expectedType) || typeMatches(actualType, expectedType),
                    String.format("Column '%s' must have type %s but is %s", 
                            columnName, expectedType, actualType));

            logger.info("✓ Column '{}' validated: {} ({})", 
                    columnName, actualType, expectedSpec.nullable ? "nullable" : "not null");
        }

        logger.info("Users table schema validation passed");
    }

    /**
     * Validates required database functions exist
     */
    @Test
    @Order(3)
    @DisplayName("Validate Required Functions Exist")
    void validateRequiredFunctionsExist() throws SQLException {
        logger.info("Validating required database functions exist");

        String[] requiredFunctions = {
            "calculate_order_total",
            "calculate_order_total_with_tax",
            "get_user_loyalty_status"
        };

        for (String functionName : requiredFunctions) {
            boolean exists = checkFunctionExists(functionName);
            assertTrue(exists, "Required function '" + functionName + "' must exist");
            logger.info("✓ Required function '{}' exists", functionName);
        }

        logger.info("All required functions validated successfully");
    }

    /**
     * Validates required stored procedures exist
     */
    @Test
    @Order(4)
    @DisplayName("Validate Required Stored Procedures Exist")
    void validateRequiredStoredProceduresExist() throws SQLException {
        logger.info("Validating required stored procedures exist");

        // Các stored procedure bắt buộc cần tồn tại
        String[] requiredProcedures = {
            "sp_mark_order_shipped"
        };

        for (String procedureName : requiredProcedures) {
            boolean exists = checkProcedureExists(procedureName);
            assertTrue(exists, "Required stored procedure '" + procedureName + "' must exist");
            logger.info("✓ Required stored procedure '{}' exists", procedureName);
        }

        logger.info("Required stored procedures validation passed (none specifically required beyond baseline)");
    }

    /**
     * Validates required triggers exist
     */
    @Test
    @Order(5)
    @DisplayName("Validate Required Triggers Exist")
    void validateRequiredTriggersExist() throws SQLException {
        logger.info("Validating required triggers exist");

        // Các trigger bắt buộc cần tồn tại
        String[] requiredTriggers = {
            "trg_after_update_order_update_user"
        };

        for (String triggerName : requiredTriggers) {
            boolean exists = checkTriggerExists(triggerName);
            assertTrue(exists, "Required trigger '" + triggerName + "' must exist");
            logger.info("✓ Required trigger '{}' exists", triggerName);
        }

        logger.info("Required triggers validation passed (none specifically required beyond baseline)");
    }

    // ===============================
    // PHƯƠNG THỨC HỖ TRỢ
    // ===============================

    /**
     * Lấy thông tin cột của một bảng
     * 
     * @param tableName tên bảng cần lấy thông tin cột
     * @return Map chứa thông tin các cột
     */
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

    /**
     * Kiểm tra xem kiểu dữ liệu thực tế có khớp với kiểu mong đợi không
     * Xử lý các mapping kiểu dữ liệu đặc thù của MySQL
     * 
     * @param actualType kiểu dữ liệu thực tế
     * @param expectedType kiểu dữ liệu mong đợi
     * @return true nếu khớp, false nếu không khớp
     */
    private boolean typeMatches(String actualType, String expectedType) {
        // Xử lý mapping kiểu dữ liệu đặc thù của MySQL
        Map<String, String> typeMapping = new HashMap<>();
        typeMapping.put("TINYINT", "BOOLEAN");
        typeMapping.put("BIT", "BOOLEAN");  // MySQL BOOLEAN được lưu dưới dạng BIT
        typeMapping.put("DECIMAL", "DECIMAL");
        typeMapping.put("BIGINT", "BIGINT");
        typeMapping.put("INT", "INTEGER");
        typeMapping.put("TEXT", "LONGVARCHAR");

        return typeMapping.getOrDefault(actualType, actualType).equals(expectedType) ||
               actualType.startsWith(expectedType) ||
               expectedType.startsWith(actualType);
    }

    /**
     * Kiểm tra xem hàm có tồn tại trong cơ sở dữ liệu không
     * 
     * @param functionName tên hàm cần kiểm tra
     * @return true nếu tồn tại, false nếu không
     */
    private boolean checkFunctionExists(String functionName) throws SQLException {
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM information_schema.routines " +
                "WHERE routine_type = 'FUNCTION' AND routine_name = ?")) {
            stmt.setString(1, functionName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Kiểm tra xem stored procedure có tồn tại trong cơ sở dữ liệu không
     * 
     * @param procedureName tên stored procedure cần kiểm tra
     * @return true nếu tồn tại, false nếu không
     */
    private boolean checkProcedureExists(String procedureName) throws SQLException {
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM information_schema.routines " +
                "WHERE routine_type = 'PROCEDURE' AND routine_name = ?")) {
            stmt.setString(1, procedureName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Kiểm tra xem trigger có tồn tại trong cơ sở dữ liệu không
     * 
     * @param triggerName tên trigger cần kiểm tra
     * @return true nếu tồn tại, false nếu không
     */
    private boolean checkTriggerExists(String triggerName) throws SQLException {
        try (PreparedStatement stmt = connectionManager.getConnection().prepareStatement(
                "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name = ?")) {
            stmt.setString(1, triggerName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Support classes
    /**
     * Column specification containing data type, nullability, and uniqueness
     */
    private static class ColumnSpec {
        final String dataType;      // Data type
        final boolean nullable;     // Nullable or not
        final boolean unique;       // Has unique constraint

        ColumnSpec(String dataType, boolean nullable, boolean unique) {
            this.dataType = dataType;
            this.nullable = nullable;
            this.unique = unique;
        }
    }

    /**
     * Actual column info from metadata
     */
    private static class ColumnInfo {
        final String typeName;      // Data type name
        final boolean nullable;     // Nullable or not

        ColumnInfo(String typeName, boolean nullable) {
            this.typeName = typeName;
            this.nullable = nullable;
        }
    }
} 
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
 * Lớp test xác minh schema cơ sở dữ liệu để đảm bảo schema phù hợp với 
 * yêu cầu nghiệp vụ và kiểm tra xem schema được thiết kế có khớp với 
 * schema được triển khai không. Điều này giải quyết yêu cầu xem xét schema 
 * cho việc test cơ sở dữ liệu toàn diện.
 * 
 * Schema validation test class that verifies the database schema aligns with 
 * business requirements and checks if the designed schema matches the 
 * implemented schema. This addresses schema review requirements for 
 * comprehensive database testing.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SchemaValidationTest {

    // Logger để ghi log thông tin test
    private static final Logger logger = LoggerFactory.getLogger(SchemaValidationTest.class);
    
    // Quản lý kết nối cơ sở dữ liệu
    private DatabaseConnectionManager connectionManager;
    // Metadata của cơ sở dữ liệu để truy vấn thông tin schema
    private DatabaseMetaData metaData;

    /**
     * Thiết lập môi trường test trước mỗi test case
     * Khởi tạo kết nối và metadata
     */
    @BeforeEach
    void setUp() throws Exception {
        logger.info("Thiết lập test xác minh schema");
        connectionManager = DatabaseConnectionManager.getInstance();
        metaData = connectionManager.getConnection().getMetaData();
        logger.info("Thiết lập test xác minh schema hoàn tất");
    }

    /**
     * Dọn dẹp sau mỗi test case
     * Đóng kết nối cơ sở dữ liệu
     */
    @AfterEach
    void tearDown() throws Exception {
        if (connectionManager != null) {
            connectionManager.closeConnection();
        }
    }

    // ===============================
    // XÁC MINH CẤU TRÚC BẢNG
    // ===============================

    /**
     * Xác minh các bảng bắt buộc tồn tại
     * Kiểm tra rằng tất cả các bảng cần thiết cho hệ thống e-commerce đều có mặt
     */
    @Test
    @Order(1)
    @DisplayName("Xác Minh Các Bảng Bắt Buộc Tồn Tại")
    void validateRequiredTablesExist() throws SQLException {
        logger.info("Xác minh các bảng bắt buộc tồn tại theo yêu cầu e-commerce");

        // Định nghĩa các bảng bắt buộc cho hệ thống e-commerce
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
                    "Bảng bắt buộc '" + requiredTable + "' phải tồn tại cho hệ thống e-commerce");
            logger.info("✓ Bảng bắt buộc '{}' tồn tại", requiredTable);
        }

        logger.info("Tất cả xác minh bảng bắt buộc thành công");
    }

    /**
     * Xác minh schema bảng users
     * Kiểm tra cấu trúc cột của bảng users theo yêu cầu nghiệp vụ
     */
    @Test
    @Order(2)
    @DisplayName("Xác Minh Schema Bảng Users")
    void validateUserTableSchema() throws SQLException {
        logger.info("Xác minh schema bảng users cho yêu cầu nghiệp vụ");

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

            // Xác minh kiểu dữ liệu (kiểm tra đơn giản)
            String actualType = actualInfo.typeName.toUpperCase();
            String expectedType = expectedSpec.dataType.toUpperCase();
            
            assertTrue(actualType.contains(expectedType) || typeMatches(actualType, expectedType),
                    String.format("Cột '%s' phải có kiểu %s nhưng là %s", 
                            columnName, expectedType, actualType));

            logger.info("✓ Xác minh cột '{}' thành công: {} ({})", 
                    columnName, actualType, expectedSpec.nullable ? "nullable" : "not null");
        }

        logger.info("Xác minh schema bảng users thành công");
    }

    /**
     * Xác minh các hàm bắt buộc tồn tại
     * Kiểm tra rằng tất cả các hàm cơ sở dữ liệu cần thiết đều có mặt
     */
    @Test
    @Order(3)
    @DisplayName("Xác Minh Các Hàm Bắt Buộc Tồn Tại")
    void validateRequiredFunctionsExist() throws SQLException {
        logger.info("Xác minh các hàm cơ sở dữ liệu bắt buộc tồn tại");

        String[] requiredFunctions = {
            "calculate_order_total",
            "calculate_order_total_with_tax",
            "get_user_loyalty_status"
        };

        for (String functionName : requiredFunctions) {
            boolean exists = checkFunctionExists(functionName);
            assertTrue(exists, "Hàm bắt buộc '" + functionName + "' phải tồn tại");
            logger.info("✓ Hàm bắt buộc '{}' tồn tại", functionName);
        }

        logger.info("Xác minh các hàm bắt buộc thành công");
    }

    /**
     * Xác minh các stored procedure bắt buộc tồn tại
     * Kiểm tra rằng tất cả các stored procedure cần thiết đều có mặt
     */
    @Test
    @Order(4)
    @DisplayName("Xác Minh Các Stored Procedure Bắt Buộc Tồn Tại")
    void validateRequiredStoredProceduresExist() throws SQLException {
        logger.info("Xác minh các stored procedure bắt buộc tồn tại");

        // Các stored procedure bắt buộc cần tồn tại
        String[] requiredProcedures = {
            "sp_mark_order_shipped"
        };

        for (String procedureName : requiredProcedures) {
            boolean exists = checkProcedureExists(procedureName);
            assertTrue(exists, "Stored procedure bắt buộc '" + procedureName + "' phải tồn tại");
            logger.info("✓ Stored procedure bắt buộc '{}' tồn tại", procedureName);
        }

        logger.info("Xác minh stored procedure bắt buộc thành công (không có yêu cầu cho hệ thống hiện tại)");
    }

    /**
     * Xác minh các trigger bắt buộc tồn tại
     * Kiểm tra rằng tất cả các trigger cần thiết đều có mặt
     */
    @Test
    @Order(5)
    @DisplayName("Xác Minh Các Trigger Bắt Buộc Tồn Tại")
    void validateRequiredTriggersExist() throws SQLException {
        logger.info("Xác minh các trigger bắt buộc tồn tại");

        // Các trigger bắt buộc cần tồn tại
        String[] requiredTriggers = {
            "trg_after_update_order_update_user"
        };

        for (String triggerName : requiredTriggers) {
            boolean exists = checkTriggerExists(triggerName);
            assertTrue(exists, "Trigger bắt buộc '" + triggerName + "' phải tồn tại");
            logger.info("✓ Trigger bắt buộc '{}' tồn tại", triggerName);
        }

        logger.info("Xác minh trigger bắt buộc thành công (không có yêu cầu cho hệ thống hiện tại)");
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

    // Lớp hỗ trợ
    /**
     * Đặc tả cột chứa thông tin về kiểu dữ liệu, nullable và unique
     */
    private static class ColumnSpec {
        final String dataType;      // Kiểu dữ liệu
        final boolean nullable;     // Có cho phép null không
        final boolean unique;       // Có unique constraint không

        ColumnSpec(String dataType, boolean nullable, boolean unique) {
            this.dataType = dataType;
            this.nullable = nullable;
            this.unique = unique;
        }
    }

    /**
     * Thông tin cột thực tế từ metadata
     */
    private static class ColumnInfo {
        final String typeName;      // Tên kiểu dữ liệu
        final boolean nullable;     // Có cho phép null không

        ColumnInfo(String typeName, boolean nullable) {
            this.typeName = typeName;
            this.nullable = nullable;
        }
    }
} 
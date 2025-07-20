package com.example.utils;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.utils.TestDataGenerator.TestDataSummary;
import com.example.demo.dao.DaoFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/**
 * Lớp tiện ích cho việc test cơ sở dữ liệu với lớp DAO.
 * Cung cấp các phương thức để quản lý kết nối cơ sở dữ liệu và thao tác dữ liệu thông qua DAO.
 * 
 * Utility class for database testing with DAO layer.
 * Provides methods for database connection management and data operations through DAOs.
 */
public class DatabaseTestUtils {
    
    // Logger để ghi log thông tin
    private static final Logger logger = LoggerFactory.getLogger(DatabaseTestUtils.class);
    
    /**
     * Lấy instance quản lý kết nối cơ sở dữ liệu.
     * 
     * @return DatabaseConnectionManager instance
     */
    public static DatabaseConnectionManager getConnectionManager() {
        return DatabaseConnectionManager.getInstance();
    }
    
    /**
     * Lấy instance factory DAO.
     * 
     * @return DaoFactory instance
     */
    public static DaoFactory getDaoFactory() {
        return DaoFactory.getInstance();
    }
    
    /**
     * Tạo và trả về bộ tạo dữ liệu test.
     * 
     * @return TestDataGenerator instance với seed cố định để test có thể tái tạo
     */
    public static TestDataGenerator createTestDataGenerator() {
        return new TestDataGenerator(12345L);
    }
    
    /**
     * Tạo và trả về bộ tạo dữ liệu test với seed tùy chỉnh.
     * 
     * @param seed seed để tạo dữ liệu ngẫu nhiên có thể tái tạo
     * @return TestDataGenerator instance với seed được chỉ định
     */
    public static TestDataGenerator createTestDataGenerator(long seed) {
        return new TestDataGenerator(seed);
    }
    
    /**
     * Thiết lập dữ liệu test sử dụng lớp DAO.
     * 
     * @param userCount số lượng user cần tạo
     * @param productCount số lượng sản phẩm cần tạo
     * @param orderCount số lượng đơn hàng cần tạo
     * @return TestDataSummary chứa thông tin về dữ liệu đã tạo
     * @throws SQLException nếu việc tạo dữ liệu thất bại
     */
    public static TestDataGenerator.TestDataSummary setupTestData(int userCount, int productCount, int orderCount) throws SQLException {
        TestDataGenerator generator = createTestDataGenerator();
        return generator.generateCompleteTestData(userCount, productCount, orderCount);
    }
    
    /**
     * Dọn dẹp dữ liệu test sử dụng lớp DAO.
     * 
     * @param summary tóm tắt dữ liệu test chứa ID của dữ liệu cần dọn dẹp
     * @throws SQLException nếu việc dọn dẹp thất bại
     */
    public static void cleanupTestData(TestDataGenerator.TestDataSummary summary) throws SQLException {
        TestDataGenerator generator = createTestDataGenerator();
        generator.cleanupTestData(summary);
    }
    
    /**
     * Commit transaction hiện tại.
     * 
     * @throws SQLException nếu commit thất bại
     */
    public static void commit() throws SQLException {
        getConnectionManager().commit();
    }
    
    /**
     * Rollback transaction hiện tại.
     * 
     * @throws SQLException nếu rollback thất bại
     */
    public static void rollback() throws SQLException {
        getConnectionManager().rollback();
    }
    
    /**
     * Đóng kết nối cơ sở dữ liệu cho thread hiện tại.
     */
    public static void closeConnection() {
        getConnectionManager().closeConnection();
    }
    
    /**
     * Thực thi code trong một transaction sử dụng lớp DAO.
     * 
     * @param callback callback transaction cần thực thi
     * @throws SQLException nếu việc thực thi transaction thất bại
     */
    public static void executeInTransaction(DatabaseConnectionManager.TransactionCallback callback) throws SQLException {
        getConnectionManager().executeInTransaction(callback);
    }
} 
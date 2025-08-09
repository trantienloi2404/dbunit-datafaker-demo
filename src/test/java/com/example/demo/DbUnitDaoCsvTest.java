package com.example.demo;

import com.example.demo.dao.DatabaseConnectionManager;
import com.example.demo.dao.UserDao;
import com.example.demo.dao.ProductDao;
import com.example.demo.dao.impl.UserDaoImpl;
import com.example.demo.dao.impl.ProductDaoImpl;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.ProductDto;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.csv.CsvURLDataSet;
import org.dbunit.ext.mysql.MySqlDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.Connection;
import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Single DBUnit test that:
 * - Uses DAO classes for all data operations (no raw SQL in the test)
 * - Loads additional rows from CSV via DBUnit
 * - Verifies using DBUnit tables without SQL queries
 * - Rolls back at the end to avoid impacting other tests
 */
public class DbUnitDaoCsvTest {

    @Test
    @DisplayName("DBUnit + DAO + CSV: insert via DAO, import CSV, verify via DBUnit, rollback")
    void dbunitDaoCsvSingleTest() throws Exception {
        DatabaseConnectionManager cm = DatabaseConnectionManager.getInstance();
        UserDao userDao = new UserDaoImpl();
        ProductDao productDao = new ProductDaoImpl();

        waitUntilDatabaseReady(userDao);

        Connection jdbc = cm.getConnection();
        IDatabaseConnection dbUnit = new DatabaseConnection(jdbc);
        dbUnit.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new MySqlDataTypeFactory());

        long usersBefore = userDao.count();
        long productsBefore = productDao.count();

        // 1) Insert one record via DAO each (no raw SQL here)
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        UserDto newUser = new UserDto();
        newUser.setUsername("dao_user_" + uniqueSuffix);
        newUser.setEmail("dao_" + uniqueSuffix + "@example.com");
        newUser.setFirstName("Dao");
        newUser.setLastName("User");
        newUser.setIsActive(true);
        userDao.create(newUser);

        ProductDto newProduct = new ProductDto();
        newProduct.setName("DAO Product " + uniqueSuffix);
        newProduct.setDescription("Inserted via DAO");
        newProduct.setPrice(new BigDecimal("9.99"));
        newProduct.setCategory("CSVDAO");
        newProduct.setSku("DAO-SKU-" + uniqueSuffix);
        newProduct.setStockQuantity(10);
        newProduct.setIsAvailable(true);
        productDao.create(newProduct);

        // 2) Load CSV dataset and IMPORT into actual tables (in same transaction)
        URL csvDir = getClass().getResource("/dbunit/csv/");
        assertNotNull(csvDir, "CSV dataset directory must exist on classpath");
        IDataSet csvDataSet = new CsvURLDataSet(csvDir);

        int csvUsers = csvDataSet.getTable("users").getRowCount();
        int csvProducts = csvDataSet.getTable("products").getRowCount();
        assertTrue(csvUsers >= 1 && csvProducts >= 1, "CSV dataset should contain sample rows");

        DatabaseOperation.INSERT.execute(dbUnit, csvDataSet);

        // 3) Verify using DBUnit without SQL: read whole tables and check counts increased
        ITable usersTable = dbUnit.createDataSet().getTable("users");
        ITable productsTable = dbUnit.createDataSet().getTable("products");

        long usersAfter = usersTable.getRowCount();
        long productsAfter = productsTable.getRowCount();

        assertEquals(usersBefore + 1 + csvUsers, usersAfter, "User count should increase by DAO+CSV rows");
        assertEquals(productsBefore + 1 + csvProducts, productsAfter, "Product count should increase by DAO+CSV rows");

        // 4) Spot-check a CSV-inserted record using DBUnit (no SQL)
        boolean foundCsvUser = false;
        for (int i = 0; i < usersTable.getRowCount(); i++) {
            Object v = usersTable.getValue(i, "username");
            if (v != null && v.toString().startsWith("csv_user_")) {
                foundCsvUser = true;
                break;
            }
        }
        assertTrue(foundCsvUser, "A csv_user_* must exist in users table after import");

        // 5) Rollback to avoid affecting the rest of the suite
        cm.rollback();
        dbUnit.close();
        cm.closeConnection();
    }

    private void waitUntilDatabaseReady(UserDao userDao) throws Exception {
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(25).toMillis();
        Exception last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                userDao.count();
                return; // DAO works => DB ready
            } catch (Exception e) {
                last = e;
                Thread.sleep(1000);
            }
        }
        if (last != null) throw last;
    }
}



# DBUnit & DataFaker Demo

A comprehensive demonstration project showcasing **DBUnit** and **DataFaker** integration for database testing with MySQL. This project demonstrates best practices for database testing, test data generation, and automated database setup/teardown.

## 🎯 Project Overview

This demo illustrates how to:
- Set up database testing infrastructure with **DBUnit**
- Generate realistic test data using **DataFaker**
- Use **Docker** for containerized MySQL testing
- Implement **Testcontainers** for isolated integration tests
- Create maintainable and reliable database tests

## 🛠️ Technologies Used

- **Java 11+**
- **Maven** for dependency management
- **DBUnit 2.7.3** for database testing framework
- **DataFaker 2.0.2** for realistic test data generation
- **MySQL 8.0** as the database
- **Docker & Docker Compose** for database containerization
- **Testcontainers** for integration testing
- **JUnit 5** for test framework
- **SLF4J + Logback** for logging

## 📋 Prerequisites

- Java 11 or higher
- Maven 3.6+
- Docker and Docker Compose
- Git

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd dbunit-datafaker-demo
```

### 2. Start MySQL Database

```bash
# Start MySQL container
docker-compose up -d

# Wait for database to be ready (check logs)
docker-compose logs -f mysql
```

### 3. Run the Tests

```bash
# Run all tests
mvn test

# Run specific test classes
mvn test -Dtest=BasicDatabaseTest
mvn test -Dtest=DataFakerIntegrationTest
mvn test -Dtest=TestcontainersIntegrationTest
```

### 4. View Test Results

```bash
# View test logs
cat target/logs/test.log

# View Maven test reports
open target/surefire-reports/index.html
```

## 📁 Project Structure

```
dbunit-datafaker-demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/          # Main application code
│   │   └── resources/
│   │       └── db/
│   │           ├── init/                   # Database initialization scripts
│   │           │   ├── 01-create-schema.sql
│   │           │   └── 02-sample-data.sql
│   │           └── migration/              # Flyway migrations
│   └── test/
│       ├── java/com/example/demo/          # Test classes
│       │   ├── BasicDatabaseTest.java      # Basic DBUnit operations
│       │   ├── DataFakerIntegrationTest.java # DataFaker integration
│       │   ├── TestcontainersIntegrationTest.java # Testcontainers demo
│       │   ├── DatabaseTestUtils.java      # DBUnit utilities
│       │   └── TestDataGenerator.java      # DataFaker test data generator
│       └── resources/
│           ├── datasets/                   # XML test datasets
│           │   ├── sample-users.xml
│           │   └── complete-test-data.xml
│           └── logback-test.xml            # Test logging configuration
├── docker-compose.yml                     # MySQL container setup
├── pom.xml                                # Maven dependencies
└── README.md                             # This file
```

## 🗄️ Database Schema

The demo uses a realistic e-commerce database schema with the following tables:

### Tables Overview

- **users** - Customer information
- **products** - Product catalog
- **orders** - Customer orders
- **order_items** - Order line items
- **reviews** - Product reviews

### Relationships

```
users (1) -----> (n) orders
users (1) -----> (n) reviews
products (1) ---> (n) order_items
products (1) ---> (n) reviews
orders (1) -----> (n) order_items
```

## 🧪 Test Classes Explained

### 1. BasicDatabaseTest

Demonstrates fundamental DBUnit operations:
- Database connection setup
- XML dataset loading
- Data setup and cleanup
- Basic data verification
- Relationship testing

```java
@Test
void testUsersTableData() throws Exception {
    ITable usersTable = testDataSet.getTable("users");
    assertEquals(3, usersTable.getRowCount());
    assertEquals("john_demo", usersTable.getValue(0, "username"));
}
```

### 2. DataFakerIntegrationTest

Shows DataFaker integration for generating realistic test data:
- Programmatic dataset generation
- Data uniqueness and integrity
- Performance testing with large datasets
- Data variety and realism validation

```java
@Test
void testMediumDatasetGeneration() throws Exception {
    IDataSet dataSet = dataGenerator.generateCompleteDataSet(50, 100, 75);
    DatabaseTestUtils.setupTestData(connection, dataSet);
    // Verify 50 users, 100 products, 75 orders
}
```

### 3. TestcontainersIntegrationTest

Demonstrates isolated testing with Testcontainers:
- Temporary MySQL container creation
- Schema initialization from scripts
- Complete isolation between tests
- Complex query testing

```java
@Container
static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
    .withDatabaseName("testcontainer_db")
    .withInitScript("db/init/01-create-schema.sql");
```

## 🔧 Key Components

### DatabaseTestUtils

Utility class providing:
- Database connection management
- DBUnit configuration for MySQL
- Dataset loading from XML files
- Data setup/cleanup operations
- Transaction management

### TestDataGenerator

DataFaker-powered test data generator:
- Realistic user profiles
- Product catalogs with variety
- Order generation with relationships
- Review data with ratings
- Configurable data volumes

## 📊 Features Demonstrated

### DBUnit Features

- **Dataset Operations**: CLEAN_INSERT, DELETE_ALL, TRUNCATE_TABLE
- **XML Datasets**: Static test data definition
- **Database Assertions**: Table content verification
- **Transaction Control**: Setup and cleanup operations
- **MySQL Integration**: Proper MySQL data type handling

### DataFaker Features

- **Realistic Data**: Names, emails, addresses, phone numbers
- **Business Logic**: Product categories, order statuses, ratings
- **Data Relationships**: Foreign key consistency
- **Uniqueness**: Unique usernames, emails, SKUs
- **Variety**: Different data distributions and ranges

### Testing Patterns

- **Setup/Teardown**: Proper test isolation
- **Data Verification**: Content and relationship validation
- **Performance Testing**: Large dataset handling
- **Integration Testing**: End-to-end database workflows

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Failed**
   ```bash
   # Check if MySQL container is running
   docker-compose ps
   
   # Check MySQL logs
   docker-compose logs mysql
   ```

2. **Tests Fail with "Table doesn't exist"**
   ```bash
   # Restart containers to reinitialize schema
   docker-compose down
   docker-compose up -d
   ```

3. **Permission Denied Errors**
   ```bash
   # Ensure Docker has proper permissions
   sudo chmod 666 /var/run/docker.sock
   ```

### Debug Mode

Run tests with debug logging:
```bash
mvn test -Dlogback.configurationFile=src/test/resources/logback-test.xml
```

## 📈 Performance Considerations

### Dataset Sizes Tested

- **Small**: 5 users, 8 products, 3 orders
- **Medium**: 50 users, 100 products, 75 orders
- **Large**: 100+ users, 200+ products, 150+ orders

### Performance Benchmarks

- Data generation: ~10-15 seconds for large datasets
- Data insertion: ~30-45 seconds for large datasets
- Complex queries: ~1-5 seconds

## 🔄 Continuous Integration

### GitHub Actions Example

```yaml
name: Database Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Run tests
        run: mvn test
```

## 📚 Learning Resources

### DBUnit
- [DBUnit Official Documentation](http://dbunit.sourceforge.net/)
- [DBUnit Best Practices](http://dbunit.sourceforge.net/bestpractices.html)

### DataFaker
- [DataFaker GitHub](https://github.com/datafaker-net/datafaker)
- [DataFaker Documentation](https://www.datafaker.net/documentation/getting-started/)

### Testcontainers
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [MySQL Testcontainers Module](https://www.testcontainers.org/modules/databases/mysql/)

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Add tests for new functionality
4. Ensure all tests pass
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🏷️ Tags

`database-testing` `dbunit` `datafaker` `mysql` `docker` `testcontainers` `junit5` `maven` `integration-testing` 
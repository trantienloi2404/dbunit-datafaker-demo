# Database Testing Demo

A comprehensive demonstration project showcasing the integration of **DBUnit** and **DataFaker** for robust database testing with MySQL. This project provides a complete example of how to create realistic test data and perform database testing using modern Java testing practices.

## 🎯 Project Overview

This demo project demonstrates:

- **DBUnit Integration**: Database testing with DBUnit for data setup, verification, and cleanup
- **DataFaker Integration**: Realistic test data generation using DataFaker library
- **DAO Pattern**: Clean data access layer implementation
- **TestContainers**: Containerized MySQL database for isolated testing
- **Comprehensive Test Coverage**: Multiple test scenarios covering various database operations

## 🏗️ Architecture

The project follows a clean architecture pattern with the following components:

```
src/
├── main/java/com/example/demo/
│   ├── dao/           # Data Access Objects (interfaces)
│   ├── dao/impl/      # DAO implementations
│   └── dto/           # Data Transfer Objects
├── main/resources/db/
│   └── init/          # Database initialization scripts
└── test/java/com/example/demo/
    ├── *.java         # Test classes
    └── utils/         # Test utilities
```

## 🚀 Features

### Database Schema
- **Users**: Customer information with authentication details
- **Products**: Product catalog with inventory management
- **Orders**: Order management with status tracking
- **Order Items**: Order line items with pricing
- **Reviews**: Product reviews and ratings

### Advanced Database Features
- **Stored Procedures**: Order processing and inventory management
- **Functions**: Loyalty status calculation and rating aggregation
- **Triggers**: Automatic timestamp updates and data validation
- **Indexes**: Performance optimization for common queries

### Testing Capabilities
- **Realistic Data Generation**: Using DataFaker for authentic test data
- **Database Isolation**: Each test runs in isolation with cleanup
- **CRUD Operations**: Complete Create, Read, Update, Delete testing
- **Data Integrity**: Foreign key and constraint validation
- **Performance Testing**: Query optimization and performance validation

## 📋 Prerequisites

- **Java 11** or higher
- **Maven 3.6** or higher
- **Docker** and **Docker Compose** (for MySQL container)
- **Git** (for cloning the repository)

## 🛠️ Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd dbunit-datafaker-demo
```

### 2. Start MySQL Database
```bash
docker-compose up -d
```

This will start a MySQL 8.0 container with the following configuration:
- **Host**: localhost
- **Port**: 3306
- **Database**: testdb
- **Username**: testuser
- **Password**: testpass

### 3. Verify Database Connection
```bash
docker-compose ps
```

The database should be running and healthy.

### 4. Build the Project
```bash
mvn clean compile
```

## 🧪 Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Classes
```bash
# Basic database operations
mvn test -Dtest=BasicDatabaseTest

# DataFaker integration tests
mvn test -Dtest=DataFakerIntegrationTest

# Database functional tests
mvn test -Dtest=DatabaseFunctionalTest

# Schema validation tests
mvn test -Dtest=SchemaValidationTest

# Query validation tests
mvn test -Dtest=QueryValidationTest
```

### Run Tests with Detailed Logging
```bash
mvn test -Dtest=BasicDatabaseTest -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG
```

## 📊 Test Categories

### 1. BasicDatabaseTest
- Database connection validation
- Basic CRUD operations
- Data consistency checks
- DAO pattern implementation

### 2. DataFakerIntegrationTest
- Realistic data generation
- Data uniqueness validation
- Data quality assessment
- Integration with DAO layer

### 3. DatabaseFunctionalTest
- Complex query testing
- Stored procedure execution
- Function testing
- Performance validation

### 4. SchemaValidationTest
- Database schema verification
- Constraint validation
- Index verification
- Foreign key integrity

### 5. QueryValidationTest
- SQL query optimization
- Query result validation
- Performance benchmarking
- Query plan analysis

## 🔧 Configuration

### Database Configuration
The database connection is configured in `DatabaseConnectionManager.java`:

```java
// Default configuration
URL: jdbc:mysql://localhost:3306/testdb
Username: testuser
Password: testpass
```

### Test Data Configuration
Test data generation is configured in `TestDataGenerator.java`:

```java
// Default test data quantities
Users: 3-20 records
Products: 5-30 records
Orders: 2-15 records
```

## 📚 Dependencies

### Core Dependencies
- **DBUnit 2.7.3**: Database testing framework
- **DataFaker 2.0.2**: Realistic test data generation
- **MySQL Connector 8.0.33**: MySQL JDBC driver
- **JUnit 5.10.1**: Testing framework

### Testing Dependencies
- **HikariCP 5.1.0**: Connection pooling
- **SLF4J 2.0.9**: Logging facade
- **Logback 1.4.14**: Logging implementation

## 🙏 Acknowledgments

- **DBUnit Team**: For the excellent database testing framework
- **DataFaker Team**: For the realistic test data generation library

**Happy Testing! 🧪✨** 
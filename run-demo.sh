#!/bin/bash

set -e

echo "🚀 DBUnit & DataFaker Demo Setup"
echo "================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 11 or higher."
        exit 1
    fi
    
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1-2)
    print_status "Java version: $java_version"
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven 3.6+."
        exit 1
    fi
    
    mvn_version=$(mvn -version | head -n1 | cut -d' ' -f3)
    print_status "Maven version: $mvn_version"
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker."
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose."
        exit 1
    fi
    
    print_status "All prerequisites satisfied ✅"
}

# Start MySQL container
start_database() {
    print_status "Starting MySQL database container..."
    
    if [ "$(docker-compose ps -q mysql)" ]; then
        print_warning "MySQL container is already running"
    else
        docker-compose up -d mysql
        print_status "MySQL container started"
    fi
    
    print_status "Waiting for MySQL to be ready..."
    max_attempts=30
    attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if docker-compose exec -T mysql mysqladmin ping -h localhost -u testuser -ptestpass --silent; then
            print_status "MySQL is ready! ✅"
            break
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            print_error "MySQL failed to start after $max_attempts attempts"
            print_error "Check logs with: docker-compose logs mysql"
            exit 1
        fi
        
        echo -n "."
        sleep 2
        ((attempt++))
    done
}

# Compile the project
compile_project() {
    print_status "Compiling project..."
    mvn clean compile test-compile
    print_status "Project compiled successfully ✅"
}

# Run tests
run_tests() {
    local test_class="$1"
    
    if [ -z "$test_class" ]; then
        print_status "Running all tests..."
        mvn test
    else
        print_status "Running test class: $test_class"
        mvn test -Dtest="$test_class"
    fi
}

# Show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -h, --help              Show this help message"
    echo "  -s, --setup-only        Only setup database, don't run tests"
    echo "  -t, --test CLASS        Run specific test class"
    echo "  -c, --clean             Clean up and stop containers"
    echo "  -l, --logs              Show MySQL container logs"
    echo ""
    echo "Test Classes:"
    echo "  BasicDatabaseTest       - Basic DBUnit operations"
    echo "  DataFakerIntegrationTest - DataFaker integration"
    echo "  TestcontainersIntegrationTest - Testcontainers demo"
    echo ""
    echo "Examples:"
    echo "  $0                      # Run full demo"
    echo "  $0 -t BasicDatabaseTest # Run only basic tests"
    echo "  $0 -s                   # Setup database only"
    echo "  $0 -c                   # Clean up"
}

# Clean up
cleanup() {
    print_status "Cleaning up..."
    docker-compose down -v
    print_status "Cleanup completed ✅"
}

# Show logs
show_logs() {
    print_status "Showing MySQL container logs..."
    docker-compose logs -f mysql
}

# Main execution
main() {
    case "${1:-}" in
        -h|--help)
            show_usage
            exit 0
            ;;
        -c|--clean)
            cleanup
            exit 0
            ;;
        -l|--logs)
            show_logs
            exit 0
            ;;
        -s|--setup-only)
            check_prerequisites
            start_database
            compile_project
            print_status "Setup completed! Database is ready for testing."
            print_status "Run 'mvn test' to execute tests."
            exit 0
            ;;
        -t|--test)
            if [ -z "${2:-}" ]; then
                print_error "Test class name required with -t option"
                show_usage
                exit 1
            fi
            check_prerequisites
            start_database
            compile_project
            run_tests "$2"
            exit 0
            ;;
        "")
            # Full demo
            check_prerequisites
            start_database
            compile_project
            run_tests
            print_status "Demo completed successfully! 🎉"
            print_status "Check test results in target/surefire-reports/"
            print_status "View logs in target/logs/test.log"
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
}

# Handle Ctrl+C
trap 'echo -e "\n${YELLOW}Demo interrupted${NC}"; exit 1' INT

# Run main function
main "$@" 
# Transaction Fraud Component
A production-grade, enterprise-ready modular Java component for processing financial transactions with comprehensive fraud detection, security hardening, and performance monitoring. Built with clean architecture principles and well-tested resilience patterns.

## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
- [Security Features](#security-features)
- [Project Architecture](#project-architecture)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)
- [Demo Scenarios](#demo-scenarios)
- [Monitoring & Metrics](#monitoring--metrics)
- [Project Status](#project-status)
- [License](#license)

## Overview
The Transaction Fraud Component is a hardened, production-ready engine for handling financial transactions with built-in fraud detection. It combines PostgreSQL for durable storage, Redis for high-speed caching, and enterprise-grade resilience patterns including:

- **Connection Pooling** (HikariCP) for optimal database performance
- **Circuit Breaker** (Resilience4j) for fault tolerance
- **Comprehensive Metrics** (Micrometer) for observability
- **Rate Limiting** to prevent abuse
- **Input Validation** for security
- **JavaFX GUI** for interactive testing and monitoring (optional)

This component allows developers to inject:

**Fraud Rules:** Custom logic to flag suspicious activity using rich metadata like device IDs and geolocation.

**Listeners:** Event handlers to react to success, failure, or fraud events (e.g., Logging, UI updates).

## Key Features

### Core Functionality
- **Hybrid Persistence Layer:** PostgreSQL with HikariCP connection pooling + Redis with circuit breaker protection
- **Metadata-Rich Transactions:** Processes User IDs, Device IDs, IP addresses, and geolocation for behavioral analysis
- **Pluggable Fraud Detection:** Implement the IFraudRule interface to create custom security rules
- **Event-Driven Architecture:** Observer pattern for real-time notifications
- **Factory Pattern:** Simplified engine configuration via TransactionEngineFactory
- **Immutable Context:** Thread-safe snapshot of transaction history

### Production-Ready Features
- **Connection Pooling:** HikariCP with configurable pool sizes, leak detection, and health monitoring
- **Circuit Breaker:** Resilience4j protection for Redis operations with automatic recovery
- **Retry Logic:** Automatic retry with exponential backoff for database deadlocks
- **Graceful Degradation:** System continues operating when cache is unavailable
- **Comprehensive Metrics:** Real-time tracking of success/failure rates, latency, and cache performance
- **Correlation IDs:** Request tracking for audit trails and debugging

## Security Features

### Input Validation
- **User ID:** 3-50 alphanumeric characters with hyphens/underscores
- **Device ID:** 3-100 alphanumeric characters
- **IP Address:** IPv4 format validation
- **Coordinates:** Latitude (-90 to 90), Longitude (-180 to 180)
- **Amount:** Positive values within configurable bounds
- **SQL Injection Protection:** All database access is fully parameterized; user input is never interpolated into SQL

### Authentication & Authorization
- **JWT Tokens:** Industry-standard tokens with configurable expiration (default 1 hour)
- **Token Validation:** Signature verification, expiration checks, subject matching
- **Failed Auth Protection:** 5-attempt lockout with 5-minute cooldown

### Rate Limiting
- **Per-User Limits:** 20 requests per minute using sliding window algorithm
- **Distributed Tracking:** In-memory tracking with ConcurrentHashMap
- **Automatic Reset:** Expired attempts automatically cleared

## Project Architecture
**Core Components `(src/main/java./.../component/core)`**
- `TransactionEngine`: The main processor. It updates and maintains the balance, coordinates persistence, caching, and rule evaluation.
- `TransactionContext`: A data transfer object passed to fraud rules containing the transaction amount and historical snapshot.

### Persistence Layer `(src/main/java/.../component/persistence)`
- `TransactionRepository`: Manages PostgreSQL operations with prepared statements
- `TransactionCacheManager`: Redis operations with circuit breaker protection and graceful degradation
- `ConnectionPoolConfig`: HikariCP configuration and management

### Security Layer `(src/main/java/.../component/security)`
- `AuthService`: Authentication interface
- `JwtService`: JWT token generation and validation with expiration
- `InputValidator`: Centralized input validation and sanitization

### Resilience Layer `(src/main/java/.../component/resilience)`
- `ResilientCircuitBreaker`: Circuit breaker implementation using Resilience4j

### Monitoring Layer `(src/main/java/.../component/monitoring)`
- `TransactionMetrics`: Comprehensive metrics collection using Micrometer

### API `(src/main/java/.../component/api)`
- `ITransactionProcessor`: The public interface that applications interact with
- `IFraudRule`: The interface for creating new fraud checks
- `TransactionListener`: The interface for receiving real-time transaction events

### Included Rules `(src/main/java/.../component/rules)`
- `LargeAmountRule`: Flags single transactions over a specific threshold
- `DailyLimitRule`: Flags usage if the total daily volume exceeds a limit
- `VelocityRule`: Flags high-frequency transactions (e.g., >3 transactions in 60 seconds)
- `ImpossibleTravelRule`: Detects physically impossible location changes
- `SpendingAnomalyRule`: Identifies unusual spending patterns
- `CardTestingRule`: Detects card testing fraud patterns

## Getting Started
**Prerequisites** 
- Java Development Kit (JDK) 17 or higher.

- Apache Maven.

- Running instances of PostgreSQL and Redis.

## Installation
1. Configure your credentials in `src/main/resources/config.properties`
2. Build the project using Maven:

Bash

```bash
mvn clean install
```
## Execution

### Option 1: JavaFX GUI (Interactive)
Launch the graphical user interface for interactive testing and monitoring:

```bash
mvn javafx:run
```

The GUI provides a dashboard for simulating transactions, viewing real-time metrics, and browsing transaction history.

### Option 2: Console Demo (Automated)
Run the automated console-based demo scenarios:

```bash
java -cp target/transaction-fraud-comp-1.0-SNAPSHOT.jar io.github.michaelcommitsat3am.transactionfraudcomp.Main
```


## Usage Guide

### 1. Database Setup
Run the schema initialization script:

```bash
psql -U postgres -d transaction_fraud -f src/main/resources/db/schema.sql
```

This creates optimized tables with indexes:
- `accounts` - User balances with audit timestamps
- `transactions` - Full transaction history with geolocation
- Indexes on `user_id`, `timestamp`, and composite keys

### 2. Configuration
Initialize infrastructure with connection pooling and metrics:

```java
// 1. Setup HikariCP Connection Pool
String jdbcUrl = "jdbc:postgresql://localhost:5432/transaction_fraud";
DataSource dataSource = ConnectionPoolConfig.createDataSource(
    jdbcUrl, "postgres", "password", "TransactionPool"
);

// 2. Setup Redis with Circuit Breaker
RedisClient redisClient = RedisClient.create("redis://localhost:6379");
StatefulRedisConnection<String, String> redisConnection = redisClient.connect();

// 3. Initialize Metrics
TransactionMetrics metrics = new TransactionMetrics();

// 4. Create Repositories
TransactionRepository repo = new TransactionRepository(dataSource);
TransactionCacheManager cache = new TransactionCacheManager(redisConnection, metrics);

// 5. Create Engine with Factory
ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(
    1000.00,  // Initial Balance
    5000.00,  // Daily Limit
    repo,     // PostgreSQL with Connection Pool
    cache,    // Redis with Circuit Breaker
    List.of(new LargeAmountRule(2000.00), new ImpossibleTravelRule()),
    List.of(new BankingEventHandler())
);
```


### 2. Defining Fraud Rules

```java
public class WeekendBlockRule implements IFraudRule {
    @Override
    public boolean isFraudulent(TransactionContext context) {
        return isWeekend(); // Logic to block weekend transactions
    }

    @Override
    public String getRuleName() {
        return "Weekend Transactions Not Allowed";
    }
}
```

### 3. Handling Events

```java
public class ConsoleLogger implements TransactionListener {
    public void onApproved(TransactionApprovedEvent e) {
        System.out.println("Success: " + e.getNewBalance());
    }
    public void onDeclined(TransactionDeclinedEvent e) {
        System.out.println("Failed: " + e.getReason());
    }
    public void onFraudDetected(FraudDetectedEvent e) {
        System.out.println("SECURITY ALERT: " + e.getFlagReason());
    }
}
```


## Demo Scenarios

### Scenario 1: Banking App (app.banking)
**Profile:** High net-worth client with multi-device access and global travel.

**Rules:** 
- LargeAmountRule ($2000)

- DailyLimitRule ($5000)

- ImpossibleTravelRule

- SpendingAnomalyRule (3.0x multiplier)

**Metadata Leveraged:** User ID, Geolocation (Lat/Lon), and Historical spending patterns.

**Behavior:** 
- **Standard Validation:** Facilitates large daily transactions while enforcing hard caps on total volume.

- **Location Awareness:** Automatically flags transactions if the speed between the last known location and the current request (e.g., New York to London) exceeds commercial flight speeds.

- **Behavioral Analysis:** Uses historical data in Redis to calculate a user's average spend; it flags "Spending Anomalies" if a request is significantly higher than the user's typical behavior, providing protection even for amounts below the standard large-amount threshold.

###  Scenario 2: ATM System (app.atm)
**Profile:** Fixed-location public terminal requiring high-frequency protection.

Rules: 
- LargeAmountRule ($200)

- VelocityRule (3 tx / 60s)

- CardTestingRule

**Metadata Leveraged:** Device ID, Terminal IP, and transaction sequence timing.

**Behavior:** - Terminal Security: Enforces strict per-transaction limits and high-frequency "brute-force" protection via the VelocityRule.

**Anti-Pattern Detection:** The CardTestingRule identifies common fraud sequences where a small "test" withdrawal (e.g., $1.00) is used to verify card validity before an immediate attempt at a large "drain" withdrawal.

**Device Locking:** In fraud scenarios, the system reacts to specific FraudDetectedEvents to retain the physical card and lock the terminal based on the unique DeviceId.

---

## Monitoring & Metrics

The system tracks comprehensive metrics using Micrometer:

### Transaction Metrics
- Success/Declined/Fraud counts
- Transaction processing latency
- Fraud rule hit rates

### Database Metrics
- Query execution count
- Query execution time
- Database error rate
- Connection pool statistics

### Cache Metrics
- Cache hit/miss ratio
- Cache operation latency
- Cache error rate
- Circuit breaker state

### Example Output
```
=== Transaction Metrics Snapshot ===
Success: 15
Declined: 3
Fraud: 2
Fraud Rule Hits: 2
DB Queries: 30
DB Errors: 0
Cache Hits: 12
Cache Misses: 3
Cache Errors: 0
Avg Transaction Time: 45.2 ms
===================================
```

---

## Project Status
✅ **Production-Ready** with comprehensive hardening:

- ✅ Security: Input validation, JWT authentication, rate limiting
- ✅ Robustness: Connection pooling, circuit breaker, retry logic
- ✅ Performance: Optimized queries, metrics tracking, connection reuse
- ✅ Build: All 35 source files compile successfully
- ✅ Testing: Unit and integration test framework in place

## Dependencies

**Core:**
- Java 17+
- PostgreSQL 15+
- Redis 6+

**Libraries:**
- HikariCP 5.1.0 (Connection Pooling)
- Resilience4j 2.2.0 (Circuit Breaker)
- Micrometer 1.12.2 (Metrics)
- Lettuce 6.3.1 (Redis Client)
- JJWT 0.12.5 (JWT Tokens)
- Jackson 2.16.1 (JSON Processing)
- SLF4J + Logback (Logging)

## License
MIT License – free to use for educational and commercial purposes.
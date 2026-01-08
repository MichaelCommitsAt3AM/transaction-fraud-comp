# Transaction Fraud Component
A modular, extensible, and reusable Java component designed to process financial transactions while enforcing configurable fraud detection rules. This project demonstrates clean architecture principles, separating core business logic from specific application implementations (like an ATM or a Banking App).
## Table of Contents
- [Overview](#overview)
- [Key Features](#key-features)
- [Project Architecture](#project-architecture)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)
- [Demo Scenarios](#demo-scenarios)
- [Project Status](#project-status)
- [License](#license)

## Overview
The Transaction Fraud Component acts as a "black box" engine for handling deposits and withdrawals. To support high-volume, production-grade scenarios, it utilizes a hybrid persistence model combining PostgreSQL for long-term audit trails and Redis for high-speed, real-time rule evaluation.

This component allows developers to inject:

**Fraud Rules:** Custom logic to flag suspicious activity using rich metadata like device IDs and geolocation.

**Listeners:** Event handlers to react to success, failure, or fraud events (e.g., Logging, UI updates).

## Key Features
- **Hybrid Persistence Layer:** Integrates PostgreSQL for durable storage and Redis (via Lettuce) for low-latency history lookups.

- **Metadata-Rich Transactions:** Processes User IDs, Device IDs, IP addresses, and geolocation to enable behavioral analysis.

- **Pluggable Fraud Detection:** Implement the IFraudRule interface to create custom security rules.

- **Event-Driven Architecture:** Uses the Observer pattern to notify applications of results in real-time.

- **Factory Pattern:** Simplifies complex engine configuration via TransactionEngineFactory.

- **Immutable Context:** Rules receive a safe snapshot of transaction history to prevent side effects.

## Project Architecture
**Core Components `(src/main/java./.../component/core)`**
- `TransactionEngine`: The main processor. It updates and maintains the balance, coordinates persistence, caching, and rule evaluation.
- `TransactionContext`: A data transfer object passed to fraud rules containing the transaction amount and historical snapshot.

## Persistence Layer `(src/main/java/.../component/persistence)`

- `TransactionRepository:` Manages PostgreSQL operations for long-term storage.

- `TransactionCacheManager:` Handles Redis operations for real-time history caching using Lettuce.

**API `(src/main/java/.../component/api)`**
- `ITransactionProcessor`: The public interface that applications interact with.
- `IFraudRule`: The interface for creating new fraud checks.
- `TransactionListener`: The interface for receiving real-time transaction events.

**Included Rules `(src/main.java./.../component/rules)`**
- `LargeAmountRule`: Flags single transactions over a specific threshold.
- `DailyLimitRule`: Flags usage if the total daily volume exceeds a limit.
- `VelocityRule`: Flags high-frequency transactions (e.g., >3 transactions in 60 seconds).

### Getting Started
**Prerequisites**  
Java Development Kit (JDK) 8 or higher.

**Installation**  
Clone the repository and compile the source code:

```bash
# Compile all Java source files into the bin directory
javac -d bin src/**/*.java

# Run the main.java./.../Main class
java -cp bin main.java./.../Main
```

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

Bash
```Bash
java -cp target/transaction-fraud-comp-1.0-SNAPSHOT.jar io.github.michaelcommitsat3am.transactionfraudcomp.Main
```


## Usage Guide

### 1. Configuration
   Initialize the repositories and use the factory to create a configured engine:

```Java

// Setup Infrastructure
TransactionRepository repo = new TransactionRepository(dataSource);
TransactionCacheManager cache = new TransactionCacheManager(redisConnection);

// Create Engine
ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(
1000.00, // Initial Balance
5000.00, // Daily Limit
repo,    // PostgreSQL Persistence
cache,   // Redis Cache
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

## Project Status
The component is fully functional with support for persistent relational storage and real-time behavioral analysis.
## License
MIT License – free to use for educational purposes.
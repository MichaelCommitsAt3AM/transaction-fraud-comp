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
The Transaction Fraud Component acts as a "black box" engine for handling deposits and withdrawals. Instead of hardcoding rules into a specific application, this component allows developers to inject:

- **Fraud Rules**: Custom logic to flag suspicious activity (e.g., Velocity checks, Daily limits).
- **Listeners**: Event handlers to react to success, failure, or fraud events (e.g., Logging, UI updates).

The project includes two proof-of-concept applications (ATMApplication and BankingApplication) to demonstrate how the same engine can be reused with completely different configurations.

## Key Features
- **Pluggable Fraud Detection**: Implement the `IFraudRule` interface to create custom security rules.
- **Event-Driven Architecture**: Uses the Observer pattern (`TransactionListener`) to notify applications of transaction results.
- **Factory Pattern**: Simplifies engine creation via `TransactionEngineFactory`.
- **Robust Error Handling**: Distinguishes between Insufficient Funds, Invalid Amounts, and Fraud Detection.
- **Immutable Context**: Fraud rules receive a safe, immutable snapshot of the transaction history to prevent side effects.

## Project Architecture
**Core Components (src/component/core)**
- `TransactionEngine`: The main processor. It manages the balance, validates transactions against rules, and dispatches events.
- `TransactionContext`: A data transfer object passed to fraud rules containing the transaction amount and history.

**API (src/component/api)**
- `ITransactionProcessor`: The public interface that applications interact with.
- `IFraudRule`: The interface for creating new fraud checks.
- `TransactionListener`: The interface for receiving real-time transaction events.

**Included Rules (src/component/rules)**
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

# Run the Main class
java -cp bin Main
```

## Usage Guide

### 1. Configuration
Use the TransactionEngineFactory to create an instance of the processor. Provide initial balance, daily limit, fraud rules, and listeners:


```java
import component.factory.TransactionEngineFactory;
import component.rules.*;
import component.api.*;
import java.util.List;

ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(
    1000.00, // Initial Balance
    5000.00, // Hard Daily Limit
    List.of(
        new LargeAmountRule(2000.00),
        new VelocityRule(3, 60) // Max 3 tx per 60 seconds
    ),
    List.of(new MyCustomListener())
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

**Profile:** High net-worth client

**Rules:** 
- LargeAmountRule ($2000) 
- DailyLimitRule ($5000)

**Behavior:** 
Allows large deposits and withdrawals but blocks massive one-time transfers.

---

### Scenario 2: ATM System (app.atm)

**Profile:** Public terminal, high security

**Rules:** 
- LargeAmountRule ($200) 
- VelocityRule (3 tx / 1 min)

**Behavior:** 
A $300 withdrawal is flagged as fraud here, whereas it would be allowed in the Banking App.

---

## Project Status
The component is fully functional and tested with demo applications. It currently uses in-memory storage for transaction history.

## License
MIT License – free to use for educational purposes.
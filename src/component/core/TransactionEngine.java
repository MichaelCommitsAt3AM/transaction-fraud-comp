package component.core;

import component.api.*;
import component.events.*;
import component.exceptions.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionEngine implements ITransactionProcessor {

    // Internal State (Encapsulated)
    private double currentBalance;
    private double dailyLimit;
    private List<Transaction> transactionHistory;

    // Pluggable Dependencies
    private List<IFraudRule> fraudRules;
    private List<TransactionListener> listeners;

    public TransactionEngine(double initialBalance, double dailyLimit) {
        this.currentBalance = initialBalance;
        this.dailyLimit = dailyLimit;
        this.transactionHistory = new ArrayList<>();
        this.fraudRules = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    // --- Configuration Methods ---
    public void addFraudRule(IFraudRule rule) {
        this.fraudRules.add(rule);
    }

    @Override
    public void addTransactionListener(TransactionListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public double getCurrentBalance() {
        return this.currentBalance;
    }

    // --- Core Logic ---
    @Override
    public void processTransaction(double amount, TransactionType type) {
        try {
            // 1. Basic Validation
            validateAmount(amount);

            // 2. Business Logic Validation (Balance Check)
            if (type == TransactionType.WITHDRAWAL) {
                validateBalance(amount);
            }

            // 3. Fraud Detection (The Strategy Pattern)
            TransactionContext context = new TransactionContext(amount, currentBalance, transactionHistory);
            checkFraud(context);

            // 4. Execution (If we get here, everything is valid)
            executeTransaction(amount, type);

        } catch (InsufficientBalanceException e) {
            notifyDeclined(amount, e.getMessage());
        } catch (FraudDetectedException e) {
            notifyFraud(amount, e.getMessage());
        } catch (InvalidAmountException e) {
            notifyDeclined(amount, e.getMessage()); // Treat invalid input as declined
        }
    }

    // --- Internal Helper Methods ---
    
    private void validateAmount(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
    }

    private void validateBalance(double amount) throws InsufficientBalanceException {
        if (amount > currentBalance) {
            throw new InsufficientBalanceException("Insufficient funds. Available: " + currentBalance);
        }
    }

    private void checkFraud(TransactionContext context) throws FraudDetectedException {
        for (IFraudRule rule : fraudRules) {
            if (rule.isFraudulent(context)) {
                throw new FraudDetectedException("Fraud detected by: " + rule.getRuleName());
            }
        }
    }

    private void executeTransaction(double amount, TransactionType type) {
        if (type == TransactionType.DEPOSIT) {
            currentBalance += amount;
        } else {
            currentBalance -= amount;
        }
        
        // Record history
        transactionHistory.add(new Transaction(amount, type));
        
        // Fire Success Event
        notifyApproved(amount, type);
    }

    // --- Event Firing Methods ---

    private void notifyApproved(double amount, TransactionType type) {
        TransactionApprovedEvent event = new TransactionApprovedEvent(amount, type, currentBalance);
        for (TransactionListener listener : listeners) listener.onApproved(event);
    }

    private void notifyDeclined(double amount, String reason) {
        TransactionDeclinedEvent event = new TransactionDeclinedEvent(amount, reason);
        for (TransactionListener listener : listeners) listener.onDeclined(event);
    }

    private void notifyFraud(double amount, String ruleName) {
        FraudDetectedEvent event = new FraudDetectedEvent(amount, ruleName);
        for (TransactionListener listener : listeners) listener.onFraudDetected(event);
    }
}
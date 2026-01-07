package component.core;

import component.api.*;
import component.events.*;
import component.exceptions.*;
import component.model.TransactionResult;
import component.model.TransactionStatus;

import java.util.ArrayList;
import java.util.List;

public class TransactionEngine implements ITransactionProcessor {

    private double currentBalance;
    private double dailyLimit;
    private List<Transaction> transactionHistory;
    private List<IFraudRule> fraudRules;
    private List<TransactionListener> listeners;

    public TransactionEngine(double initialBalance, double dailyLimit) {
        this.currentBalance = initialBalance;
        this.dailyLimit = dailyLimit;
        this.transactionHistory = new ArrayList<>();
        this.fraudRules = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public void addFraudRule(IFraudRule rule) {
        this.fraudRules.add(rule);
    }

    @Override
    public void addTransactionListener(TransactionListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeTransactionListener(TransactionListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public double getCurrentBalance() {
        return this.currentBalance;
    }

    @Override
    public TransactionResult processTransaction(double amount, TransactionType type, double lat, double lon) {
        try {
            validateAmount(amount);

            if (type == TransactionType.WITHDRAWAL) {
                validateBalance(amount);
            }

            // Pass real coordinates to the Context
            TransactionContext context = new TransactionContext(amount, currentBalance, transactionHistory, lat, lon);
            checkFraud(context);

            executeTransaction(amount, type, lat, lon);

            return new TransactionResult(TransactionStatus.SUCCESS, "Transaction Approved", currentBalance);

        } catch (InsufficientBalanceException | InvalidAmountException e) {
            notifyDeclined(amount, e.getMessage());
            return new TransactionResult(TransactionStatus.DECLINED, e.getMessage(), currentBalance);

        } catch (FraudDetectedException e) {
            notifyFraud(amount, e.getMessage());
            return new TransactionResult(TransactionStatus.FRAUD_DETECTED, e.getMessage(), currentBalance);

        } catch (Exception e) {
            return new TransactionResult(TransactionStatus.ERROR, "System Error: " + e.getMessage(), currentBalance);
        }
    }

    private void validateAmount(double amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive");
    }

    private void validateBalance(double amount) throws InsufficientBalanceException {
        if (amount > currentBalance) throw new InsufficientBalanceException("Insufficient funds");
    }

    private void checkFraud(TransactionContext context) throws FraudDetectedException {
        for (IFraudRule rule : fraudRules) {
            if (rule.isFraudulent(context)) {
                throw new FraudDetectedException(rule.getRuleName());
            }
        }
    }

    private void executeTransaction(double amount, TransactionType type, double lat, double lon) {
        if (type == TransactionType.DEPOSIT) {
            currentBalance += amount;
        } else {
            currentBalance -= amount;
        }
        // Store coordinates in history
        transactionHistory.add(new Transaction(amount, type, lat, lon));
        notifyApproved(amount, type);
    }

    // --- Event Helpers (Simplified) ---
    private void notifyApproved(double amount, TransactionType type) {
        TransactionApprovedEvent event = new TransactionApprovedEvent(amount, type, currentBalance);
        listeners.forEach(l -> l.onApproved(event));
    }

    private void notifyDeclined(double amount, String reason) {
        TransactionDeclinedEvent event = new TransactionDeclinedEvent(amount, reason);
        listeners.forEach(l -> l.onDeclined(event));
    }

    private void notifyFraud(double amount, String ruleName) {
        FraudDetectedEvent event = new FraudDetectedEvent(amount, ruleName);
        listeners.forEach(l -> l.onFraudDetected(event));
    }
}
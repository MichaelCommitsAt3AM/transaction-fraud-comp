package io.github.michaelcommitsat3am.transactionfraudcomp.component.core;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.events.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.exceptions.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class TransactionEngine implements ITransactionProcessor {

    private double currentBalance;
    private double dailyLimit;
    private List<IFraudRule> fraudRules;
    private List<TransactionListener> listeners;

    // New dependencies
    private TransactionRepository repository;
    private TransactionCacheManager cacheManager;

    public TransactionEngine(double initialBalance, double dailyLimit,
                             TransactionRepository repo, TransactionCacheManager cache) {
        this.currentBalance = initialBalance;
        this.dailyLimit = dailyLimit;
        this.repository = repo;
        this.cacheManager = cache;
        this.fraudRules = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public void addFraudRule(IFraudRule rule) { this.fraudRules.add(rule); }

    @Override
    public void addTransactionListener(TransactionListener listener) { this.listeners.add(listener); }

    @Override
    public void removeTransactionListener(TransactionListener listener) { this.listeners.remove(listener); }

    @Override
    public double getCurrentBalance() { return this.currentBalance; }

    // Updated Signature to accept Metadata
    @Override
    public TransactionResult processTransaction(String userId, double amount, TransactionType type,
                                                double lat, double lon, String deviceId, String ip) {

        String location = String.format("%.4f, %.4f", lat, lon);

        // 1. Create Transaction Object
        Transaction tx = new Transaction(userId, amount, type, location, lat, lon, deviceId, ip, "Standard");

        try {
            validateAmount(amount);
            if (type == TransactionType.WITHDRAWAL) validateBalance(amount);

            // 2. Persist & Cache (Log the attempt)
            // Note: In a real distributed system, consider eventual consistency or a saga pattern.
            repository.saveTransaction(tx);
            cacheManager.addTransaction(tx);

            // 3. Fetch History for Context (From Redis for speed)
            List<Transaction> recentHistory = cacheManager.getRecentTransactions(userId);

            // 4. Build Context
            TransactionContext context = new TransactionContext(
                    amount, currentBalance, recentHistory, lat, lon, location
            );

            // 5. Evaluate Rules
            checkFraud(context);

            // 6. Execute (Update Balance)
            executeTransaction(amount, type);

            return new TransactionResult(TransactionStatus.SUCCESS, "Approved", currentBalance);

        } catch (Exception e) {
            handleException(e, amount);
            if (e instanceof FraudDetectedException) {
                return new TransactionResult(TransactionStatus.FRAUD_DETECTED, e.getMessage(), currentBalance);
            }
            return new TransactionResult(TransactionStatus.DECLINED, e.getMessage(), currentBalance);
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

    private void executeTransaction(double amount, TransactionType type) {
        if (type == TransactionType.DEPOSIT) currentBalance += amount;
        else currentBalance -= amount;

        // Notify listeners
        listeners.forEach(l -> l.onApproved(new TransactionApprovedEvent(amount, type, currentBalance)));
    }

    private void handleException(Exception e, double amount) {
        if (e instanceof FraudDetectedException) {
            listeners.forEach(l -> l.onFraudDetected(new FraudDetectedEvent(amount, e.getMessage())));
        } else {
            listeners.forEach(l -> l.onDeclined(new TransactionDeclinedEvent(amount, e.getMessage())));
        }
    }
}
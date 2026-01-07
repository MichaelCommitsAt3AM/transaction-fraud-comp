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

    // IMPROVEMENT: Allow removing listeners
    @Override
    public void removeTransactionListener(TransactionListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public double getCurrentBalance() {
        return this.currentBalance;
    }

    @Override
    public TransactionResult processTransaction(double amount, TransactionType type) {
        try {
            validateAmount(amount);

            if (type == TransactionType.WITHDRAWAL) {
                validateBalance(amount);
            }

            TransactionContext context = new TransactionContext(amount, currentBalance, transactionHistory);
            checkFraud(context);

            executeTransaction(amount, type);

            return new TransactionResult(TransactionStatus.SUCCESS, "Transaction Approved", currentBalance);

        } catch (InsufficientBalanceException e) {
            notifyDeclined(amount, e.getMessage());
            return new TransactionResult(TransactionStatus.DECLINED, e.getMessage(), currentBalance);

        } catch (FraudDetectedException e) {
            notifyFraud(amount, e.getMessage());
            return new TransactionResult(TransactionStatus.FRAUD_DETECTED, e.getMessage(), currentBalance);

        } catch (InvalidAmountException e) {
            notifyDeclined(amount, e.getMessage());
            return new TransactionResult(TransactionStatus.DECLINED, e.getMessage(), currentBalance);
        } catch (Exception e) {
            return new TransactionResult(TransactionStatus.ERROR, "System Error: " + e.getMessage(), currentBalance);
        }
    }

    private void validateAmount(double amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
    }

    private void validateBalance(double amount) throws InsufficientBalanceException {
        if (amount > currentBalance) {
            throw new InsufficientBalanceException("Insufficient funds");
        }
    }

    private void checkFraud(TransactionContext context) throws FraudDetectedException {
        for (IFraudRule rule : fraudRules) {
            if (rule.isFraudulent(context)) {
                throw new FraudDetectedException(rule.getRuleName());
            }
        }
    }

    private void executeTransaction(double amount, TransactionType type) {
        if (type == TransactionType.DEPOSIT) {
            currentBalance += amount;
        } else {
            currentBalance -= amount;
        }
        transactionHistory.add(new Transaction(amount, type));
        notifyApproved(amount, type);
    }

    // --- Robust Event Dispatching (IMPROVEMENT 4) ---
    private void notifyListeners(TransactionEvent event, java.util.function.BiConsumer<TransactionListener, TransactionEvent> action) {
        for (TransactionListener listener : new ArrayList<>(listeners)) { // Iterate over copy to avoid ConcurrentModification
            try {
                action.accept(listener, event);
            } catch (Exception e) {
                System.err.println("[Component] Listener failed: " + e.getMessage());
                // Don't rethrow, keep notifying others
            }
        }
    }

    private void notifyApproved(double amount, TransactionType type) {
        TransactionApprovedEvent event = new TransactionApprovedEvent(amount, type, currentBalance);
        notifyListeners(event, (l, e) -> l.onApproved((TransactionApprovedEvent) e));
    }

    private void notifyDeclined(double amount, String reason) {
        TransactionDeclinedEvent event = new TransactionDeclinedEvent(amount, reason);
        notifyListeners(event, (l, e) -> l.onDeclined((TransactionDeclinedEvent) e));
    }

    private void notifyFraud(double amount, String ruleName) {
        FraudDetectedEvent event = new FraudDetectedEvent(amount, ruleName);
        notifyListeners(event, (l, e) -> l.onFraudDetected((FraudDetectedEvent) e));
    }
}
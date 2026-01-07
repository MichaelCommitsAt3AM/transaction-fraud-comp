package component.core;

import java.util.List;

public class TransactionContext {
    private final double amount;
    private final double currentBalance;
    private final List<Transaction> history;

    public TransactionContext(double amount, double currentBalance, List<Transaction> history) {
        this.amount = amount;
        this.currentBalance = currentBalance;
        this.history = history;
    }

    public double getAmount() { return amount; }
    public double getCurrentBalance() { return currentBalance; }
    public List<Transaction> getHistory() { return history; }
}
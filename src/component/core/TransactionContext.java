package component.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable context passed to Fraud Rules.
 * Prevents rules from modifying internal engine state.
 */
public class TransactionContext {
    private final double amount;
    private final double currentBalance;
    private final List<Transaction> history;

    public TransactionContext(double amount, double currentBalance, List<Transaction> history) {
        this.amount = amount;
        this.currentBalance = currentBalance;
        // Defensive copy to prevent mutation of the engine's internal list
        this.history = new ArrayList<>(history);
    }

    public double getAmount() { return amount; }
    public double getCurrentBalance() { return currentBalance; }

    // IMPROVEMENT: Return unmodifiable view
    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
package component.core;

import java.time.LocalDateTime;

public class Transaction {
    private final double amount;
    private final TransactionType type;
    private final LocalDateTime timestamp;

    public Transaction(double amount, TransactionType type) {
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // Getters omitted for brevity
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
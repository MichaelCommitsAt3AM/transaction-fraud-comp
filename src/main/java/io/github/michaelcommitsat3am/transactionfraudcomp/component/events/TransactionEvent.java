package io.github.michaelcommitsat3am.transactionfraudcomp.component.events;

import java.time.LocalDateTime;

public abstract class TransactionEvent {
    private final double amount;
    private final LocalDateTime timestamp;

    public TransactionEvent(double amount) {
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
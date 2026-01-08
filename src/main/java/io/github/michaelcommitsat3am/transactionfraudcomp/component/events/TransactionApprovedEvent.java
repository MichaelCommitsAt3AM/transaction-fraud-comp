package io.github.michaelcommitsat3am.transactionfraudcomp.component.events;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;

public class TransactionApprovedEvent extends TransactionEvent {
    private final TransactionType type;
    private final double newBalance;

    public TransactionApprovedEvent(double amount, TransactionType type, double newBalance) {
        super(amount);
        this.type = type;
        this.newBalance = newBalance;
    }

    public TransactionType getType() { return type; }
    public double getNewBalance() { return newBalance; }

    @Override
    public String toString() {
        return "SUCCESS: " + getType() + " of $" + getAmount() + ". New Balance: $" + newBalance;
    }
}
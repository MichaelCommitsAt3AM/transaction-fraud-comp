package io.github.michaelcommitsat3am.transactionfraudcomp.component.model;

public class TransactionResult {
    private final TransactionStatus status;
    private final String message;
    private final double newBalance;

    public TransactionResult(TransactionStatus status, String message, double newBalance) {
        this.status = status;
        this.message = message;
        this.newBalance = newBalance;
    }

    public TransactionStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public double getNewBalance() { return newBalance; }

    public boolean isSuccess() {
        return status == TransactionStatus.SUCCESS;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (Bal: $%.2f)", status, message, newBalance);
    }
}
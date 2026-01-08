package io.github.michaelcommitsat3am.transactionfraudcomp.component.events;

public class FraudDetectedEvent extends TransactionEvent {
    private final String flagReason;

    public FraudDetectedEvent(double amount, String flagReason) {
        super(amount);
        this.flagReason = flagReason;
    }

    public String getFlagReason() { return flagReason; }

    @Override
    public String toString() {
        return "FRAUD ALERT: Transaction of $" + getAmount() + " flagged. Rule: " + flagReason;
    }
}
package component.events;

public class TransactionDeclinedEvent extends TransactionEvent {
    private final String reason;

    public TransactionDeclinedEvent(double amount, String reason) {
        super(amount);
        this.reason = reason;
    }

    public String getReason() { return reason; }
    
    @Override
    public String toString() {
        return "DECLINED: $" + getAmount() + ". Reason: " + reason;
    }
}
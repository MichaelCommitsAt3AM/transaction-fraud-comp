package app.banking;

import component.api.TransactionListener;
import component.events.*;

public class BankingEventHandler implements TransactionListener {

    @Override
    public void onApproved(TransactionApprovedEvent event) {
        System.out.println("[App] ✅ APPROVED: " + event.getType() + " $" + event.getAmount());
        System.out.println("       New Balance: $" + event.getNewBalance());
    }

    @Override
    public void onDeclined(TransactionDeclinedEvent event) {
        System.out.println("[App] ❌ DECLINED: $" + event.getAmount());
        System.out.println("       Reason: " + event.getReason());
    }

    @Override
    public void onFraudDetected(FraudDetectedEvent event) {
        System.out.println("[App] 🚨 SECURITY ALERT: $" + event.getAmount() + " flagged!");
        System.out.println("       Reason: " + event.getFlagReason());
        System.out.println("       Action: Account Locked pending review.");
    }
}
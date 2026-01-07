package app.atm;

import component.api.TransactionListener;
import component.core.TransactionEngine;
import component.core.TransactionType;
import component.events.*;
import component.rules.LargeAmountRule;

public class ATMApplication {
    
    private TransactionEngine engine;

    public ATMApplication() {
        // ATM starts with $500 user balance
        this.engine = new TransactionEngine(500.00, 1000.00);
        
        // ATM has stricter rules! Max withdrawal $200.
        this.engine.addFraudRule(new LargeAmountRule(200.00));
        
        // Inline Listener for variety (Java 8+ lambda style or anonymous class)
        this.engine.addTransactionListener(new TransactionListener() {
            public void onApproved(TransactionApprovedEvent e) { System.out.println("[ATM] Dispensing Cash..."); }
            public void onDeclined(TransactionDeclinedEvent e) { System.out.println("[ATM] Transaction Canceled: " + e.getReason()); }
            public void onFraudDetected(FraudDetectedEvent e) { System.out.println("[ATM] CARD RETAINED. Fraud suspected."); }
        });
        
        System.out.println("--- ATM System Initialized (Max Withdraw: $200) ---");
    }

    public void withdrawCash(double amount) {
        System.out.println("\n[ATM] User inserting card for: $" + amount);
        engine.processTransaction(amount, TransactionType.WITHDRAWAL);
    }
}
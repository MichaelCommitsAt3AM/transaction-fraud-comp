package app.atm;

import component.api.ITransactionProcessor;
import component.api.TransactionListener;
import component.core.TransactionType;
import component.events.*;
import component.factory.TransactionEngineFactory;
import component.rules.LargeAmountRule;
import component.rules.VelocityRule;

public class ATMApplication {

    private ITransactionProcessor engine;

    public ATMApplication() {
        // Use Factory
        // Adding a Velocity Rule: Max 3 transactions in 60 seconds
        this.engine = TransactionEngineFactory.createConfiguredEngine(
                500.00,
                1000.00,
                new component.api.IFraudRule[]{
                        new LargeAmountRule(200.00),
                        new VelocityRule(3, 60)
                },
                new TransactionListener() {
                    public void onApproved(TransactionApprovedEvent e) { System.out.println("[ATM] Dispensing Cash..."); }
                    public void onDeclined(TransactionDeclinedEvent e) { System.out.println("[ATM] Canceled: " + e.getReason()); }
                    public void onFraudDetected(FraudDetectedEvent e) { System.out.println("[ATM] CARD RETAINED."); }
                }
        );

        System.out.println("--- ATM System Initialized ---");
    }

    public void withdrawCash(double amount) {
        System.out.println("\n[ATM] User inserting card for: $" + amount);
        var result = engine.processTransaction(amount, TransactionType.WITHDRAWAL);

        // Example of using the new Result return type
        if (!result.isSuccess()) {
            System.out.println("[ATM Display] " + result.getMessage());
        }
    }
}
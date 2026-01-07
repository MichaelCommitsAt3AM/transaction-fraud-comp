package app.atm;

import component.api.ITransactionProcessor;
import component.api.TransactionListener;
import component.core.TransactionType;
import component.events.*;
import component.factory.TransactionEngineFactory;
import component.rules.CardTestingRule;
import component.rules.LargeAmountRule;
import component.rules.VelocityRule;

import java.util.List;

public class ATMApplication {

    private final ITransactionProcessor engine;

    public ATMApplication() {
        this.engine = TransactionEngineFactory.createConfiguredEngine(
                500.00,
                1000.00,
                List.of(
                        new LargeAmountRule(200.00),
                        new VelocityRule(3, 60),
                        new CardTestingRule()
                ),
                List.of(new TransactionListener() {
                    public void onApproved(TransactionApprovedEvent e) { System.out.println("[ATM] Dispensing Cash..."); }
                    public void onDeclined(TransactionDeclinedEvent e) { System.out.println("[ATM] Canceled: " + e.getReason()); }
                    public void onFraudDetected(FraudDetectedEvent e) { System.out.println("[ATM] 🚨 CARD RETAINED."); }
                })
        );
        System.out.println("--- ATM System Initialized ---");
    }

    public void withdrawCash(double amount, double lat, double lon) {
        System.out.printf("\n[ATM] Withdrawal: $%.2f at [%.4f, %.4f]%n", amount, lat, lon);
        var result = engine.processTransaction(amount, TransactionType.WITHDRAWAL, lat, lon);

        if (!result.isSuccess()) {
            System.out.println("[ATM Display] " + result.getMessage());
        }
    }
}
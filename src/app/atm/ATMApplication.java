package app.atm;

import component.api.ITransactionProcessor;
import component.api.TransactionListener;
import component.core.TransactionType;
import component.events.*;
import component.factory.TransactionEngineFactory;
import component.rules.LargeAmountRule;
import component.rules.VelocityRule;

import java.util.List;

public class ATMApplication {

    private final ITransactionProcessor engine;

    public ATMApplication() {

        this.engine = TransactionEngineFactory.createConfiguredEngine(
                500.00,        // Initial balance
                1000.00,       // Daily withdrawal limit
                List.of(
                        new LargeAmountRule(200.00),
                        new VelocityRule(3, 60)
                ),
                List.of(
                        new TransactionListener() {

                            @Override
                            public void onApproved(TransactionApprovedEvent event) {
                                System.out.println("[ATM] Dispensing Cash...");
                            }

                            @Override
                            public void onDeclined(TransactionDeclinedEvent event) {
                                System.out.println("[ATM] Transaction Canceled: " + event.getReason());
                            }

                            @Override
                            public void onFraudDetected(FraudDetectedEvent event) {
                                System.out.println("[ATM] FRAUD DETECTED – CARD RETAINED.");
                            }
                        }
                )
        );

        System.out.println("--- ATM System Initialized ---");
    }

    public void withdrawCash(double amount) {
        System.out.println("\n[ATM] User requesting withdrawal: $" + amount);

        var result = engine.processTransaction(amount, TransactionType.WITHDRAWAL);

        if (!result.isSuccess()) {
            System.out.println("[ATM Display] " + result.getMessage());
        }
    }
}

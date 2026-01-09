package io.github.michaelcommitsat3am.transactionfraudcomp.app.atm;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.ITransactionProcessor;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.factory.TransactionEngineFactory;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.rules.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.TransactionListener;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.events.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionResult;
import java.util.List;

public class ATMApplication {

    private final ITransactionProcessor engine;

    // Updated Constructor
    public ATMApplication(TransactionRepository repo, TransactionCacheManager cache) {
        this.engine = TransactionEngineFactory.createConfiguredEngine(
                500.00,
                1000.00,
                repo,
                cache,
                List.of(
                        new LargeAmountRule(200.00),
                        new VelocityRule(3, 60),
                        new CardTestingRule()),
                List.of(new TransactionListener() {
                    public void onApproved(TransactionApprovedEvent e) {
                        System.out.println("[ATM] Dispensing Cash...");
                    }

                    public void onDeclined(TransactionDeclinedEvent e) {
                        System.out.println("[ATM] Canceled: " + e.getReason());
                    }

                    public void onFraudDetected(FraudDetectedEvent e) {
                        System.out.println("[ATM] 🚨 CARD RETAINED.");
                    }
                }));
        System.out.println("--- ATM System Initialized ---");
    }

    public void withdrawCash(String userId, double amount, double lat, double lon, String deviceId, String ip) {
        System.out.printf("\n[ATM] Withdrawal: $%.2f at [%.4f, %.4f]%n", amount, lat, lon);
        TransactionResult result = engine.processTransaction(null, userId, amount, TransactionType.WITHDRAWAL, lat, lon,
                deviceId, ip);

        if (!result.isSuccess()) {
            System.out.println("[ATM Display] " + result.getMessage());
        }
    }
}
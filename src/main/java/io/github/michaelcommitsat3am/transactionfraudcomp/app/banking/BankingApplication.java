package io.github.michaelcommitsat3am.transactionfraudcomp.app.banking;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.ITransactionProcessor;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.factory.TransactionEngineFactory;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.rules.*;
import java.util.List;

public class BankingApplication {

    private final ITransactionProcessor transactionProcessor;

    // Updated Constructor to accept Persistence Layers
    public BankingApplication(TransactionRepository repo, TransactionCacheManager cache) {
        this.transactionProcessor = TransactionEngineFactory.createConfiguredEngine(
                1000.00,
                5000.00,
                repo,   // New dependency
                cache,  // New dependency
                List.of(
                        new LargeAmountRule(2000.00),
                        new DailyLimitRule(5000.00),
                        new ImpossibleTravelRule(),
                        new SpendingAnomalyRule(3.0)
                ),
                List.of(new BankingEventHandler())
        );
        System.out.println("--- Banking App Initialized (Balance: $1000) ---");
    }

    // Updated methods to accept metadata
    public void deposit(String userId, double amount, double lat, double lon, String deviceId, String ip) {
        System.out.printf("\nRequesting Deposit: %.2f for %s%n", amount, userId);
        transactionProcessor.processTransaction(userId, amount, TransactionType.DEPOSIT, lat, lon, deviceId, ip);
    }

    public void withdraw(String userId, double amount, double lat, double lon, String deviceId, String ip) {
        System.out.printf("\nRequesting Withdrawal: %.2f for %s%n", amount, userId);
        transactionProcessor.processTransaction(userId, amount, TransactionType.WITHDRAWAL, lat, lon, deviceId, ip);
    }
}
package app.banking;

import component.api.ITransactionProcessor;
import component.core.TransactionType;
import component.factory.TransactionEngineFactory;
import component.rules.DailyLimitRule;
import component.rules.ImpossibleTravelRule;
import component.rules.LargeAmountRule;
import component.rules.SpendingAnomalyRule;

import java.util.List;

public class BankingApplication {

    private final ITransactionProcessor transactionProcessor;

    public BankingApplication() {
        this.transactionProcessor = TransactionEngineFactory.createConfiguredEngine(
                1000.00,
                5000.00,
                List.of(
                        new LargeAmountRule(2000.00),
                        new DailyLimitRule(5000.00),
                        new ImpossibleTravelRule(),      // Uses real Lat/Lon
                        new SpendingAnomalyRule(3.0)
                ),
                List.of(new BankingEventHandler())
        );
        System.out.println("--- Banking App Initialized (Balance: $1000) ---");
    }

    // Updated to accept coordinates
    public void deposit(double amount, double lat, double lon) {
        System.out.printf("\nRequesting Deposit: %.2f at [%.4f, %.4f]%n", amount, lat, lon);
        transactionProcessor.processTransaction(amount, TransactionType.DEPOSIT, lat, lon);
    }

    public void withdraw(double amount, double lat, double lon) {
        System.out.printf("\nRequesting Withdrawal: %.2f at [%.4f, %.4f]%n", amount, lat, lon);
        transactionProcessor.processTransaction(amount, TransactionType.WITHDRAWAL, lat, lon);
    }
}
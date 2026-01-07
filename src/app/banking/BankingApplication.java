package app.banking;

import component.api.ITransactionProcessor;
import component.core.TransactionType;
import component.factory.TransactionEngineFactory;
import component.rules.DailyLimitRule;
import component.rules.LargeAmountRule;

import java.util.List;

public class BankingApplication {

    private final ITransactionProcessor transactionProcessor;

    public BankingApplication() {
        this.transactionProcessor =
                TransactionEngineFactory.createConfiguredEngine(
                        1000.00,
                        5000.00,
                        List.of(
                                new LargeAmountRule(2000.00),
                                new DailyLimitRule(5000.00)
                        ),
                        List.of(new BankingEventHandler())
                );

        System.out.println("--- Banking App Initialized (Balance: $1000) ---");
    }

    public void deposit(double amount) {
        System.out.println("\nRequesting Deposit: " + amount);
        transactionProcessor.processTransaction(amount, TransactionType.DEPOSIT);
    }

    public void withdraw(double amount) {
        System.out.println("\nRequesting Withdrawal: " + amount);
        transactionProcessor.processTransaction(amount, TransactionType.WITHDRAWAL);
    }
}

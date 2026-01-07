package app.banking;

import component.core.TransactionEngine;
import component.core.TransactionType;
import component.rules.DailyLimitRule;
import component.rules.LargeAmountRule;

public class BankingApplication {
    
    private final TransactionEngine transactionEngine;

    public BankingApplication() {
        // 1. Instantiate the Component (Initial Balance: $1000, Daily Limit Field: $5000)
        // Note: The daily limit field in constructor is for the internal check, 
        // but we also add a Rule for strict enforcement.
        this.transactionEngine = new TransactionEngine(1000.00, 5000.00);

        // 2. Wire up the Listener (The "Output")
        this.transactionEngine.addTransactionListener(new BankingEventHandler());

        // 3. Inject Rules (The "Configuration")
        // Bank allows up to $2000 per transaction
        this.transactionEngine.addFraudRule(new LargeAmountRule(2000.00));
        // Bank allows $5000 total per day
        this.transactionEngine.addFraudRule(new DailyLimitRule(5000.00));
        
        System.out.println("--- Banking App Initialized (Balance: $1000) ---");
    }

    // A simple method to trigger the component
    public void deposit(double amount) {
        System.out.println("\nRequesting Deposit: " + amount);
        transactionEngine.processTransaction(amount, TransactionType.DEPOSIT);
    }

    public void withdraw(double amount) {
        System.out.println("\nRequesting Withdrawal: " + amount);
        transactionEngine.processTransaction(amount, TransactionType.WITHDRAWAL);
    }
}
package io.github.michaelcommitsat3am.transactionfraudcomp.component.rules;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.IFraudRule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionContext;

public class DailyLimitRule implements IFraudRule {
    private final double dailyLimit;

    public DailyLimitRule(double dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    @Override
    public boolean isFraudulent(TransactionContext context) {
        double currentTotal = 0;

        // Sum up logic: Iterate through history provided by the engine
        for (Transaction t : context.getHistory()) {
            // In a real main.java.io.github.michaelcommitsat3am.transactionfraudcomp.app, you would check t.getTimestamp() vs LocalDate.now()
            // For this academic demo, we assume history is relevant or simple sum
            currentTotal += t.getAmount();
        }

        // Check if current transaction + previous history exceeds limit
        return (currentTotal + context.getAmount()) > dailyLimit;
    }

    @Override
    public String getRuleName() {
        return "Daily Limit Exceeded (Limit: " + dailyLimit + ")";
    }
}
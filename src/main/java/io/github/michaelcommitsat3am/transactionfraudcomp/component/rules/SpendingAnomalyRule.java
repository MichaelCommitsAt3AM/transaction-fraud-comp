package io.github.michaelcommitsat3am.transactionfraudcomp.component.rules;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.IFraudRule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionContext;

public class SpendingAnomalyRule implements IFraudRule {
    private final double sensitivityMultiplier; // e.g., 3.0 means "3x the average"

    public SpendingAnomalyRule(double sensitivityMultiplier) {
        this.sensitivityMultiplier = sensitivityMultiplier;
    }

    @Override
    public boolean isFraudulent(TransactionContext context) {
        if (context.getHistory().isEmpty()) return false;

        // 1. Calculate Average Spending
        double total = 0;
        for (Transaction t : context.getHistory()) {
            total += t.getAmount();
        }
        double avg = total / context.getHistory().size();

        // 2. Normalize: If average is very low (e.g. $5), don't flag a $20 lunch.
        // Set a minimum baseline of $50 for the average.
        if (avg < 50.0) avg = 50.0;

        // 3. Check if current amount exceeds the threshold
        return context.getAmount() > (avg * sensitivityMultiplier);
    }

    @Override
    public String getRuleName() {
        return "Behavioral Anomaly (Spending > " + sensitivityMultiplier + "x user average)";
    }
}
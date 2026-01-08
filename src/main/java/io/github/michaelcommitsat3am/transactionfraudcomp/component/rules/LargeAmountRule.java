package io.github.michaelcommitsat3am.transactionfraudcomp.component.rules;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.IFraudRule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionContext;

public class LargeAmountRule implements IFraudRule {
    private final double threshold;

    public LargeAmountRule(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public boolean isFraudulent(TransactionContext context) {
        return context.getAmount() > threshold;
    }

    @Override
    public String getRuleName() {
        return "Large Amount Detection (> " + threshold + ")";
    }
}
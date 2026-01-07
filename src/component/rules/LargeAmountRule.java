package component.rules;

import component.api.IFraudRule;
import component.core.TransactionContext;

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
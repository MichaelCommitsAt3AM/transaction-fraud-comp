package io.github.michaelcommitsat3am.transactionfraudcomp.component.rules;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.IFraudRule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionContext;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Advanced Rule: Detects too many transactions within a short time window.
 */
public class VelocityRule implements IFraudRule {
    private final int maxTransactions;
    private final int timeWindowSeconds;

    public VelocityRule(int maxTransactions, int timeWindowSeconds) {
        this.maxTransactions = maxTransactions;
        this.timeWindowSeconds = timeWindowSeconds;
    }

    @Override
    public boolean isFraudulent(TransactionContext context) {
        LocalDateTime now = LocalDateTime.now();
        long count = context.getHistory().stream()
                .filter(t -> ChronoUnit.SECONDS.between(t.getTimestamp(), now) <= timeWindowSeconds)
                .count();

        return count >= maxTransactions;
    }

    @Override
    public String getRuleName() {
        return "Velocity Limit (" + maxTransactions + " tx in " + timeWindowSeconds + "s)";
    }
}
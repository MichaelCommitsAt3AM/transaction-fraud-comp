package component.rules;

import component.api.IFraudRule;
import component.core.Transaction;
import component.core.TransactionContext;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;

public class CardTestingRule implements IFraudRule {

    @Override
    public boolean isFraudulent(TransactionContext context) {
        if (context.getHistory().isEmpty()) return false;

        Transaction lastTx = context.getHistory().get(context.getHistory().size() - 1);
        LocalDateTime now = LocalDateTime.now();

        // Condition 1: Happened very recently (within 5 mins)
        boolean isRapid = ChronoUnit.MINUTES.between(lastTx.getTimestamp(), now) < 5;

        // Condition 2: Last tx was tiny (under $5.00)
        boolean lastWasSmall = lastTx.getAmount() < 5.00;

        // Condition 3: Current tx is large (over $100.00)
        boolean currentIsLarge = context.getAmount() > 100.00;

        return isRapid && lastWasSmall && currentIsLarge;
    }

    @Override
    public String getRuleName() {
        return "Card Testing Pattern (Small auth followed by large drain)";
    }
}
package component.factory;

import component.api.IFraudRule;
import component.api.ITransactionProcessor;
import component.api.TransactionListener;
import component.core.TransactionEngine;

import java.util.List;

/**
 * Factory responsible for creating and configuring transaction engines.
 * Ensures consumers never depend on concrete implementations.
 */
public final class TransactionEngineFactory {

    private TransactionEngineFactory() {
        // Prevent instantiation
    }

    public static ITransactionProcessor createConfiguredEngine(
            double initialBalance,
            double dailyLimit,
            List<IFraudRule> fraudRules,
            List<TransactionListener> listeners
    ) {
        TransactionEngine engine = new TransactionEngine(initialBalance, dailyLimit);

        if (fraudRules != null) {
            fraudRules.forEach(engine::addFraudRule);
        }

        if (listeners != null) {
            listeners.forEach(engine::addTransactionListener);
        }

        return engine;
    }
}

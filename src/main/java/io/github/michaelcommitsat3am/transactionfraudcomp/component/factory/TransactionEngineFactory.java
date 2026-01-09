package io.github.michaelcommitsat3am.transactionfraudcomp.component.factory;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.IFraudRule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.ITransactionProcessor;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.TransactionListener;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionEngine;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.security.AuthService;

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
            TransactionRepository repo, // <--- Added Argument
            TransactionCacheManager cache, // <--- Added Argument
            List<IFraudRule> fraudRules,
            List<TransactionListener> listeners) {
        // Create a no-op auth service for testing/demo purposes
        AuthService authService = new AuthService() {
            @Override
            public void validateRequest(String token, String userId) throws SecurityException {
                // No-op: Accept all requests for demo/testing
            }
        };

        // Passing correct parameters: dailyLimit, repo, cache, authService
        TransactionEngine engine = new TransactionEngine(dailyLimit, repo, cache, authService);

        if (fraudRules != null) {
            fraudRules.forEach(engine::addFraudRule);
        }

        if (listeners != null) {
            listeners.forEach(engine::addTransactionListener);
        }

        return engine;
    }
}
package component.api;

import component.core.TransactionType;
import component.model.TransactionResult;

/**
 * Public API for processing transactions.
 * Applications should strictly program to this interface.
 */
public interface ITransactionProcessor {

    /**
     * Processes a transaction and returns a detailed result.
     * @param amount The transaction amount.
     * @param type The type of transaction (DEPOSIT, WITHDRAWAL).
     * @return TransactionResult containing status, message, and new balance.
     */
    TransactionResult processTransaction(double amount, TransactionType type);

    /**
     * @return The current available balance.
     */
    double getCurrentBalance();

    /**
     * Registers a listener for transaction events.
     * @param listener The listener to add.
     */
    void addTransactionListener(TransactionListener listener);

    /**
     * Removes a registered listener.
     * @param listener The listener to remove.
     */
    void removeTransactionListener(TransactionListener listener);
}
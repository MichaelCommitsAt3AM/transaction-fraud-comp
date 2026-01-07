// ITransactionProcessor.java
package component.api;

import component.core.TransactionContext;
import component.core.TransactionType;
import component.events.*;

public interface ITransactionProcessor {
    void processTransaction(double amount, TransactionType type);
    double getCurrentBalance();

    // Observer Pattern Registration
    void addTransactionListener(TransactionListener listener);
}


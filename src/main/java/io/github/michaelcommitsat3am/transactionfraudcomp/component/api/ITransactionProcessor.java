package io.github.michaelcommitsat3am.transactionfraudcomp.component.api;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionResult;

public interface ITransactionProcessor {
    // Added authToken parameter
    TransactionResult processTransaction(String authToken, String userId, double amount, TransactionType type,
                                         double lat, double lon, String deviceId, String ip);

    double getCurrentBalance();
    void addTransactionListener(TransactionListener listener);
    void removeTransactionListener(TransactionListener listener);
}
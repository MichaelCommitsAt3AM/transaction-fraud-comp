package io.github.michaelcommitsat3am.transactionfraudcomp.gui.service;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.ITransactionProcessor;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionResult;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 * JavaFX Service for processing transactions on a background thread.
 * This ensures the UI remains responsive during database and Redis operations.
 */
public class TransactionService extends Service<TransactionResult> {

    private final ITransactionProcessor processor;
    private String authToken;
    private String userId;
    private double amount;
    private TransactionType type;
    private double latitude;
    private double longitude;
    private String deviceId;
    private String ipAddress;

    public TransactionService(ITransactionProcessor processor) {
        this.processor = processor;
    }

    /**
     * Sets the transaction parameters before calling restart().
     */
    public void setTransactionParams(String authToken, String userId, double amount,
            TransactionType type, double latitude, double longitude,
            String deviceId, String ipAddress) {
        this.authToken = authToken;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
    }

    @Override
    protected Task<TransactionResult> createTask() {
        return new Task<TransactionResult>() {
            @Override
            protected TransactionResult call() throws Exception {
                // This runs on a background thread
                return processor.processTransaction(
                        authToken, userId, amount, type,
                        latitude, longitude, deviceId, ipAddress);
            }
        };
    }
}

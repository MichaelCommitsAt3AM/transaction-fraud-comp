package io.github.michaelcommitsat3am.transactionfraudcomp.gui.service;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.model.TransactionDisplayModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.List;
import java.util.stream.Collectors;

/**
 * JavaFX Service for loading transaction history on a background thread.
 */
public class HistoryLoadService extends Service<ObservableList<TransactionDisplayModel>> {

    private final TransactionRepository repository;
    private String userId;
    private int limit;

    public HistoryLoadService(TransactionRepository repository) {
        this.repository = repository;
        this.limit = 100; // Default limit
    }

    /**
     * Sets the parameters for loading history.
     */
    public void setParams(String userId, int limit) {
        this.userId = userId;
        this.limit = limit;
    }

    @Override
    protected Task<ObservableList<TransactionDisplayModel>> createTask() {
        return new Task<ObservableList<TransactionDisplayModel>>() {
            @Override
            protected ObservableList<TransactionDisplayModel> call() throws Exception {
                // This runs on a background thread
                List<Transaction> history = repository.getHistoryByUser(userId, limit);

                // Convert to display models
                List<TransactionDisplayModel> displayModels = history.stream()
                        .map(t -> new TransactionDisplayModel(
                                t.getTransactionId(),
                                t.getUserId(),
                                t.getType(),
                                t.getAmount(),
                                t.getLocation(),
                                "SUCCESS", // Simplified - ideally would track status in DB
                                t.getTimestamp()))
                        .collect(Collectors.toList());

                return FXCollections.observableArrayList(displayModels);
            }
        };
    }
}

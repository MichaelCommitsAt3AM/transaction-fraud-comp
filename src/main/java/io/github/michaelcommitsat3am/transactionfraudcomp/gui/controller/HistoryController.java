package io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.model.TransactionDisplayModel;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.service.HistoryLoadService;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.util.UIUtils;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;

/**
 * Controller for the Transaction History view.
 */
public class HistoryController {

    @FXML
    private TableView<TransactionDisplayModel> historyTable;
    @FXML
    private TableColumn<TransactionDisplayModel, String> idColumn;
    @FXML
    private TableColumn<TransactionDisplayModel, String> userColumn;
    @FXML
    private TableColumn<TransactionDisplayModel, String> typeColumn;
    @FXML
    private TableColumn<TransactionDisplayModel, Double> amountColumn;
    @FXML
    private TableColumn<TransactionDisplayModel, String> locationColumn;
    @FXML
    private TableColumn<TransactionDisplayModel, String> statusColumn;
    @FXML
    private TableColumn<TransactionDisplayModel, java.time.LocalDateTime> timestampColumn;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private Label statusLabel;

    private final HistoryLoadService historyLoadService;
    private final String defaultUserId = "user_alice_001"; // Default for demo

    public HistoryController(TransactionRepository repository) {
        this.historyLoadService = new HistoryLoadService(repository);
        setupServiceHandlers();
    }

    @FXML
    public void initialize() {
        // Configure amount column to show currency format
        amountColumn.setCellFactory(column -> new TableCell<TransactionDisplayModel, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(UIUtils.formatCurrency(amount));
                }
            }
        });

        // Configure timestamp column to show formatted date
        timestampColumn.setCellFactory(column -> new TableCell<TransactionDisplayModel, java.time.LocalDateTime>() {
            @Override
            protected void updateItem(java.time.LocalDateTime timestamp, boolean empty) {
                super.updateItem(timestamp, empty);
                if (empty || timestamp == null) {
                    setText(null);
                } else {
                    setText(UIUtils.formatTimestamp(timestamp));
                }
            }
        });

        // Apply row styling based on status
        historyTable.setRowFactory(tv -> new TableRow<TransactionDisplayModel>() {
            @Override
            protected void updateItem(TransactionDisplayModel item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setStyle("");
                    getStyleClass().removeAll("table-row-success", "table-row-fraud", "table-row-declined");
                } else {
                    // Apply status-specific styling
                    getStyleClass().removeAll("table-row-success", "table-row-fraud", "table-row-declined");
                    String styleClass = item.getStatusStyleClass();
                    if (!styleClass.isEmpty()) {
                        getStyleClass().add(styleClass);
                    }
                }
            }
        });

        // Load initial data
        handleRefresh();
    }

    @FXML
    public void handleRefresh() {
        loadingIndicator.setVisible(true);
        statusLabel.setText("Loading transaction history...");

        historyLoadService.setParams(defaultUserId, 100);
        historyLoadService.restart();
    }

    private void setupServiceHandlers() {
        historyLoadService.setOnSucceeded(event -> {
            ObservableList<TransactionDisplayModel> transactions = historyLoadService.getValue();
            Platform.runLater(() -> {
                historyTable.setItems(transactions);
                loadingIndicator.setVisible(false);
                statusLabel.setText("Loaded " + transactions.size() + " transactions");
            });
        });

        historyLoadService.setOnFailed(event -> {
            Platform.runLater(() -> {
                loadingIndicator.setVisible(false);
                statusLabel.setText("Failed to load history");
                statusLabel.getStyleClass().add("status-fraud");
            });
        });
    }
}

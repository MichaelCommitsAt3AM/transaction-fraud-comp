package io.github.michaelcommitsat3am.transactionfraudcomp.gui.model;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import javafx.beans.property.*;

import java.time.LocalDateTime;

/**
 * UI-friendly model for displaying transactions in a TableView.
 */
public class TransactionDisplayModel {

    private final StringProperty transactionId;
    private final StringProperty userId;
    private final StringProperty type;
    private final DoubleProperty amount;
    private final StringProperty location;
    private final StringProperty status;
    private final ObjectProperty<LocalDateTime> timestamp;

    public TransactionDisplayModel(String transactionId, String userId, TransactionType type,
            double amount, String location, String status, LocalDateTime timestamp) {
        this.transactionId = new SimpleStringProperty(transactionId);
        this.userId = new SimpleStringProperty(userId);
        this.type = new SimpleStringProperty(type.toString());
        this.amount = new SimpleDoubleProperty(amount);
        this.location = new SimpleStringProperty(location);
        this.status = new SimpleStringProperty(status);
        this.timestamp = new SimpleObjectProperty<>(timestamp);
    }

    // Property getters for JavaFX binding
    public StringProperty transactionIdProperty() {
        return transactionId;
    }

    public StringProperty userIdProperty() {
        return userId;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public DoubleProperty amountProperty() {
        return amount;
    }

    public StringProperty locationProperty() {
        return location;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public ObjectProperty<LocalDateTime> timestampProperty() {
        return timestamp;
    }

    // Value getters
    public String getTransactionId() {
        return transactionId.get();
    }

    public String getUserId() {
        return userId.get();
    }

    public String getType() {
        return type.get();
    }

    public double getAmount() {
        return amount.get();
    }

    public String getLocation() {
        return location.get();
    }

    public String getStatus() {
        return status.get();
    }

    public LocalDateTime getTimestamp() {
        return timestamp.get();
    }

    /**
     * Gets the CSS style class based on status.
     */
    public String getStatusStyleClass() {
        return switch (status.get().toUpperCase()) {
            case "SUCCESS" -> "table-row-success";
            case "FRAUD" -> "table-row-fraud";
            case "DECLINED" -> "table-row-declined";
            default -> "";
        };
    }
}

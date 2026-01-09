package io.github.michaelcommitsat3am.transactionfraudcomp.gui.model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * UI-friendly model for displaying metrics and performance data.
 */
public class MetricsDisplayModel {

    private final IntegerProperty successCount;
    private final IntegerProperty declinedCount;
    private final IntegerProperty fraudCount;
    private final ObservableMap<String, Integer> fraudRuleHits;
    private final DoubleProperty avgTransactionTime;
    private final StringProperty dbStatus;
    private final StringProperty cacheStatus;

    public MetricsDisplayModel() {
        this.successCount = new SimpleIntegerProperty(0);
        this.declinedCount = new SimpleIntegerProperty(0);
        this.fraudCount = new SimpleIntegerProperty(0);
        this.fraudRuleHits = FXCollections.observableHashMap();
        this.avgTransactionTime = new SimpleDoubleProperty(0.0);
        this.dbStatus = new SimpleStringProperty("Unknown");
        this.cacheStatus = new SimpleStringProperty("Unknown");
    }

    // Property getters
    public IntegerProperty successCountProperty() {
        return successCount;
    }

    public IntegerProperty declinedCountProperty() {
        return declinedCount;
    }

    public IntegerProperty fraudCountProperty() {
        return fraudCount;
    }

    public ObservableMap<String, Integer> getFraudRuleHits() {
        return fraudRuleHits;
    }

    public DoubleProperty avgTransactionTimeProperty() {
        return avgTransactionTime;
    }

    public StringProperty dbStatusProperty() {
        return dbStatus;
    }

    public StringProperty cacheStatusProperty() {
        return cacheStatus;
    }

    // Value getters
    public int getSuccessCount() {
        return successCount.get();
    }

    public int getDeclinedCount() {
        return declinedCount.get();
    }

    public int getFraudCount() {
        return fraudCount.get();
    }

    public double getAvgTransactionTime() {
        return avgTransactionTime.get();
    }

    public String getDbStatus() {
        return dbStatus.get();
    }

    public String getCacheStatus() {
        return cacheStatus.get();
    }

    // Setters
    public void setSuccessCount(int value) {
        successCount.set(value);
    }

    public void setDeclinedCount(int value) {
        declinedCount.set(value);
    }

    public void setFraudCount(int value) {
        fraudCount.set(value);
    }

    public void setAvgTransactionTime(double value) {
        avgTransactionTime.set(value);
    }

    public void setDbStatus(String value) {
        dbStatus.set(value);
    }

    public void setCacheStatus(String value) {
        cacheStatus.set(value);
    }

    /**
     * Gets total transaction count.
     */
    public int getTotalCount() {
        return successCount.get() + declinedCount.get() + fraudCount.get();
    }

    /**
     * Gets success percentage.
     */
    public double getSuccessPercentage() {
        int total = getTotalCount();
        return total > 0 ? (successCount.get() * 100.0 / total) : 0.0;
    }

    /**
     * Gets fraud percentage.
     */
    public double getFraudPercentage() {
        int total = getTotalCount();
        return total > 0 ? (fraudCount.get() * 100.0 / total) : 0.0;
    }

    /**
     * Gets CSS style class for DB status.
     */
    public String getDbStatusStyleClass() {
        return "Healthy".equalsIgnoreCase(dbStatus.get()) ? "status-success" : "status-fraud";
    }

    /**
     * Gets CSS style class for Cache status.
     */
    public String getCacheStatusStyleClass() {
        return "Healthy".equalsIgnoreCase(cacheStatus.get()) ? "status-success" : "status-fraud";
    }
}

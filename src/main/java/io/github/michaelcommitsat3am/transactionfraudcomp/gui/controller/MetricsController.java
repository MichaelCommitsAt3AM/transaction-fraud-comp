package io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.monitoring.TransactionMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.util.Duration;

/**
 * Controller for the Metrics view displaying system performance and statistics.
 */
public class MetricsController {

    @FXML
    private PieChart statusPieChart;
    @FXML
    private BarChart<String, Number> fraudRuleBarChart;
    @FXML
    private Label avgTimeLabel;
    @FXML
    private Label dbStatusLabel;
    @FXML
    private Label cacheStatusLabel;
    @FXML
    private Label successCountLabel;
    @FXML
    private Label declinedCountLabel;
    @FXML
    private Label fraudCountLabel;
    @FXML
    private Label refreshLabel;

    private final TransactionMetrics metrics;
    private Timeline autoRefreshTimeline;

    public MetricsController(TransactionMetrics metrics) {
        this.metrics = metrics;
    }

    @FXML
    public void initialize() {
        // Set up auto-refresh timer (every 3 seconds)
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> refreshMetrics()));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        // Initial refresh
        refreshMetrics();
    }

    /**
     * Refreshes metrics from the backend.
     */
    public void refreshMetrics() {
        Platform.runLater(() -> {
            updateCounts();
            updateCharts();
            updatePerformanceMetrics();
        });
    }

    private void updateCounts() {
        try {
            // Get counter values from metrics
            double successCount = getCounterValue("transactions.success");
            double declinedCount = getCounterValue("transactions.declined");
            double fraudCount = getCounterValue("transactions.fraud");

            successCountLabel.setText(String.format("%.0f", successCount));
            declinedCountLabel.setText(String.format("%.0f", declinedCount));
            fraudCountLabel.setText(String.format("%.0f", fraudCount));
        } catch (Exception e) {
            // If metrics unavailable, show 0
            successCountLabel.setText("0");
            declinedCountLabel.setText("0");
            fraudCountLabel.setText("0");
        }
    }

    private void updateCharts() {
        try {
            double successCount = getCounterValue("transactions.success");
            double declinedCount = getCounterValue("transactions.declined");
            double fraudCount = getCounterValue("transactions.fraud");

            // Update Pie Chart
            statusPieChart.getData().clear();
            if (successCount > 0) {
                statusPieChart.getData().add(new PieChart.Data("Success", successCount));
            }
            if (declinedCount > 0) {
                statusPieChart.getData().add(new PieChart.Data("Declined", declinedCount));
            }
            if (fraudCount > 0) {
                statusPieChart.getData().add(new PieChart.Data("Fraud", fraudCount));
            }

            // Update Bar Chart (Fraud Rule Hits)
            double fraudRuleHits = getCounterValue("fraud.rules.hit");

            fraudRuleBarChart.getData().clear();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.getData().add(new XYChart.Data<>("Total Fraud Hits", fraudRuleHits > 0 ? fraudRuleHits : 0));
            fraudRuleBarChart.getData().add(series);

        } catch (Exception e) {
            // Charts remain empty if no data
        }
    }

    private void updatePerformanceMetrics() {
        try {
            // Get average transaction time
            double avgTime = getTimerMean("transactions.processing");
            avgTimeLabel.setText(String.format("%.2f ms", avgTime));

            // Database status (simplified - check if queries are running)
            double dbQueries = getCounterValue("database.queries");
            double dbErrors = getCounterValue("database.errors");
            boolean dbHealthy = dbErrors == 0 || (dbQueries > 0 && dbErrors / dbQueries < 0.1);

            dbStatusLabel.setText(dbHealthy ? "Healthy" : "Issues Detected");
            dbStatusLabel.getStyleClass().clear();
            dbStatusLabel.getStyleClass().add(dbHealthy ? "status-success" : "status-fraud");

            // Cache status
            double cacheHits = getCounterValue("cache.hits");
            double cacheMisses = getCounterValue("cache.misses");
            double cacheErrors = getCounterValue("cache.errors");
            boolean cacheHealthy = cacheErrors == 0 || (cacheHits + cacheMisses > 0 &&
                    cacheErrors / (cacheHits + cacheMisses) < 0.1);

            cacheStatusLabel.setText(cacheHealthy ? "Healthy" : "Issues Detected");
            cacheStatusLabel.getStyleClass().clear();
            cacheStatusLabel.getStyleClass().add(cacheHealthy ? "status-success" : "status-fraud");

        } catch (Exception e) {
            avgTimeLabel.setText("N/A");
            dbStatusLabel.setText("Unknown");
            cacheStatusLabel.setText("Unknown");
        }
    }

    private double getCounterValue(String name) {
        try {
            Counter counter = metrics.getRegistry().find(name).counter();
            return counter != null ? counter.count() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getTimerMean(String name) {
        try {
            Timer timer = metrics.getRegistry().find(name).timer();
            if (timer != null && timer.count() > 0) {
                return timer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Ignore
        }
        return 0.0;
    }

    /**
     * Stops the auto-refresh when the controller is no longer needed.
     */
    public void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }
    }
}

package io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Main controller managing navigation between different views.
 */
public class MainController {

    @FXML
    private Button dashboardBtn;
    @FXML
    private Button metricsBtn;
    @FXML
    private Button historyBtn;
    @FXML
    private StackPane contentArea;
    @FXML
    private Label systemStatusLabel;

    private Node dashboardView;
    private Node metricsView;
    private Node historyView;

    private DashboardController dashboardController;
    private MetricsController metricsController;
    private HistoryController historyController;

    /**
     * Sets the view instances (injected from Application).
     */
    public void setViews(Node dashboardView, DashboardController dashboardController,
            Node metricsView, MetricsController metricsController,
            Node historyView, HistoryController historyController) {
        this.dashboardView = dashboardView;
        this.dashboardController = dashboardController;
        this.metricsView = metricsView;
        this.metricsController = metricsController;
        this.historyView = historyView;
        this.historyController = historyController;

        // Show dashboard by default
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        switchView(dashboardView);
        setActiveButton(dashboardBtn);
    }

    @FXML
    public void showMetrics() {
        switchView(metricsView);
        setActiveButton(metricsBtn);

        // Refresh metrics when view is shown
        if (metricsController != null) {
            metricsController.refreshMetrics();
        }
    }

    @FXML
    public void showHistory() {
        switchView(historyView);
        setActiveButton(historyBtn);

        // Refresh history when view is shown
        if (historyController != null) {
            historyController.handleRefresh();
        }
    }

    private void switchView(Node view) {
        contentArea.getChildren().clear();
        if (view != null) {
            contentArea.getChildren().add(view);
        }
    }

    private void setActiveButton(Button activeButton) {
        // Remove active class from all buttons
        dashboardBtn.getStyleClass().remove("nav-button-active");
        metricsBtn.getStyleClass().remove("nav-button-active");
        historyBtn.getStyleClass().remove("nav-button-active");

        // Add active class to the selected button
        if (!activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    /**
     * Updates the system status label.
     */
    public void setSystemStatus(String status, boolean healthy) {
        String icon = healthy ? "🟢" : "🔴";
        systemStatusLabel.setText(icon + " " + status);
        systemStatusLabel.getStyleClass().clear();
        systemStatusLabel.getStyleClass().add(healthy ? "status-success" : "status-fraud");
    }
}

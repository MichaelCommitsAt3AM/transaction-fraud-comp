package io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.ITransactionProcessor;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.TransactionListener;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.events.FraudDetectedEvent;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.events.TransactionApprovedEvent;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.events.TransactionDeclinedEvent;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionResult;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.service.TransactionService;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.util.UIUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Controller for the Dashboard/Simulation view implementing transaction
 * listener.
 */
public class DashboardController implements TransactionListener {

    @FXML
    private ToggleButton atmModeToggle;
    @FXML
    private ToggleButton bankingModeToggle;
    @FXML
    private Label modeDescriptionLabel;

    @FXML
    private TextField userIdField;
    @FXML
    private TextField deviceIdField;
    @FXML
    private TextField ipAddressField;

    @FXML
    private Slider latitudeSlider;
    @FXML
    private Slider longitudeSlider;
    @FXML
    private Label latitudeLabel;
    @FXML
    private Label longitudeLabel;

    @FXML
    private TextField amountField;
    @FXML
    private Button depositBtn;
    @FXML
    private Button withdrawBtn;

    @FXML
    private VBox feedbackPanel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private ProgressIndicator progressIndicator;

    private final ITransactionProcessor processor;
    private final TransactionService transactionService;
    private Stage stage;

    // Location constants
    private static final double NY_LAT = 40.7128;
    private static final double NY_LON = -74.0060;
    private static final double LONDON_LAT = 51.5074;
    private static final double LONDON_LON = -0.1278;
    private static final double TOKYO_LAT = 35.6762;
    private static final double TOKYO_LON = 139.6503;

    public DashboardController(ITransactionProcessor processor) {
        this.processor = processor;
        this.transactionService = new TransactionService(processor);
        setupServiceHandlers();

        // Register as listener
        processor.addTransactionListener(this);
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        // Set up toggle group for mode selection
        ToggleGroup modeGroup = new ToggleGroup();
        atmModeToggle.setToggleGroup(modeGroup);
        bankingModeToggle.setToggleGroup(modeGroup);

        // Set up mode change listeners
        modeGroup.selectedToggleProperty().addListener((obs, old, newToggle) -> {
            if (newToggle == atmModeToggle) {
                modeDescriptionLabel.setText("ATM mode with strict limits and velocity checks");
            } else {
                modeDescriptionLabel.setText("Banking mode with higher limits and advanced fraud detection");
            }
        });

        // Set default values
        userIdField.setText("user_alice_001");
        deviceIdField.setText("device_mobile_01");
        ipAddressField.setText("192.168.1.100");

        // Bind slider labels to slider values
        latitudeSlider.valueProperty()
                .addListener((obs, old, newVal) -> latitudeLabel.setText(String.format("%.2f", newVal.doubleValue())));

        longitudeSlider.valueProperty()
                .addListener((obs, old, newVal) -> longitudeLabel.setText(String.format("%.2f", newVal.doubleValue())));

        // Initialize labels
        latitudeLabel.setText(String.format("%.2f", latitudeSlider.getValue()));
        longitudeLabel.setText(String.format("%.2f", longitudeSlider.getValue()));

        // Update balance display
        updateBalanceDisplay();
    }

    @FXML
    public void handleDeposit() {
        processTransaction(TransactionType.DEPOSIT);
    }

    @FXML
    public void handleWithdraw() {
        processTransaction(TransactionType.WITHDRAWAL);
    }

    @FXML
    public void setNewYorkLocation() {
        latitudeSlider.setValue(NY_LAT);
        longitudeSlider.setValue(NY_LON);
        showFeedback("Location set to New York 🗽", false);
    }

    @FXML
    public void setLondonLocation() {
        latitudeSlider.setValue(LONDON_LAT);
        longitudeSlider.setValue(LONDON_LON);
        showFeedback("Location set to London 🌉", false);
    }

    @FXML
    public void setTokyoLocation() {
        latitudeSlider.setValue(TOKYO_LAT);
        longitudeSlider.setValue(TOKYO_LON);
        showFeedback("Location set to Tokyo 🌆", false);
    }

    private void processTransaction(TransactionType type) {
        try {
            // Validate inputs
            String userId = userIdField.getText().trim();
            String deviceId = deviceIdField.getText().trim();
            String ip = ipAddressField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().trim());
            double lat = latitudeSlider.getValue();
            double lon = longitudeSlider.getValue();

            if (userId.isEmpty() || deviceId.isEmpty() || ip.isEmpty()) {
                showFeedback("Please fill in all user/device fields", true);
                return;
            }

            if (amount <= 0) {
                showFeedback("Amount must be positive", true);
                return;
            }

            // Show progress
            progressIndicator.setVisible(true);
            showFeedback("Processing " + type.toString().toLowerCase() + " of $" +
                    String.format("%.2f", amount) + "...", false);

            // Process on background thread
            String authToken = "demo_token"; // Simplified for demo
            transactionService.setTransactionParams(authToken, userId, amount, type, lat, lon, deviceId, ip);
            transactionService.restart();

        } catch (NumberFormatException e) {
            showFeedback("Invalid amount format", true);
        } catch (Exception e) {
            showFeedback("Error: " + e.getMessage(), true);
        }
    }

    private void setupServiceHandlers() {
        transactionService.setOnSucceeded(event -> {
            TransactionResult result = transactionService.getValue();
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                updateBalanceDisplay();

                // Feedback is handled by listener callbacks
            });
        });

        transactionService.setOnFailed(event -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                Throwable exception = transactionService.getException();
                showFeedback("Transaction failed: " + exception.getMessage(), true);
            });
        });
    }

    private void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().clear();
        feedbackLabel.getStyleClass().add(isError ? "status-fraud" : "status-success");
    }

    private void updateBalanceDisplay() {
        try {
            double balance = processor.getCurrentBalance();
            balanceLabel.setText("Current Balance: " + UIUtils.formatCurrency(balance));
        } catch (Exception e) {
            balanceLabel.setText("Current Balance: N/A");
        }
    }

    // ===== TransactionListener Implementation =====

    @Override
    public void onApproved(TransactionApprovedEvent event) {
        Platform.runLater(() -> {
            String message = String.format("✅ SUCCESS: %s approved. New balance: %s",
                    event.getType(),
                    UIUtils.formatCurrency(event.getNewBalance()));
            showFeedback(message, false);
            updateBalanceDisplay();

            // Show toast notification
            if (stage != null) {
                UIUtils.showToast(stage, "Transaction Approved!", UIUtils.ToastType.SUCCESS);
            }
        });
    }

    @Override
    public void onDeclined(TransactionDeclinedEvent event) {
        Platform.runLater(() -> {
            String message = String.format("⚠️ DECLINED: %s", event.getReason());
            showFeedback(message, true);

            // Show toast notification
            if (stage != null) {
                UIUtils.showToast(stage, "Transaction Declined: " + event.getReason(),
                        UIUtils.ToastType.WARNING);
            }
        });
    }

    @Override
    public void onFraudDetected(FraudDetectedEvent event) {
        Platform.runLater(() -> {
            String message = String.format("🚨 FRAUD DETECTED: %s", event.getFlagReason());
            showFeedback(message, true);

            // Flash red animation
            UIUtils.fraudFlashEffect(feedbackPanel);

            // Show toast notification
            if (stage != null) {
                UIUtils.showToast(stage, "FRAUD: " + event.getFlagReason(),
                        UIUtils.ToastType.FRAUD);
            }
        });
    }
}

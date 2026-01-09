package io.github.michaelcommitsat3am.transactionfraudcomp.gui.util;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utility class for common UI operations.
 */
public class UIUtils {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);

    /**
     * Displays a toast notification at the bottom of the stage.
     */
    public static void showToast(Stage stage, String message, ToastType type) {
        Popup popup = new Popup();

        Label label = new Label(message);
        label.setStyle(
                "-fx-background-color: " + getToastColor(type) + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-padding: 15px 20px;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;");

        StackPane pane = new StackPane(label);
        pane.setAlignment(Pos.BOTTOM_CENTER);
        popup.getContent().add(pane);

        // Position at bottom center
        popup.setAutoHide(true);
        double x = stage.getX() + stage.getWidth() / 2 - 150;
        double y = stage.getY() + stage.getHeight() - 100;

        popup.show(stage, x, y);

        // Auto-hide after 3 seconds
        Timeline timeline = new Timeline(new KeyFrame(
                Duration.seconds(3),
                e -> popup.hide()));
        timeline.play();
    }

    /**
     * Shows an alert dialog.
     */
    public static void showAlert(String title, String header, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        // Apply dark mode styling
        alert.getDialogPane().getStylesheets().add(
                UIUtils.class.getResource("/css/styles.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("panel");

        alert.showAndWait();
    }

    /**
     * Creates a "fraud flash" animation effect on a node.
     */
    public static void fraudFlashEffect(Node node) {
        String originalStyle = node.getStyle();

        // Flash red 3 times
        Timeline timeline = new Timeline();
        for (int i = 0; i < 3; i++) {
            int index = i;
            timeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.millis(index * 300), e -> node.setStyle(originalStyle +
                            "-fx-background-color: #dc3545; " +
                            "-fx-effect: dropshadow(gaussian, #dc3545, 20, 0.7, 0, 0);")),
                    new KeyFrame(Duration.millis(index * 300 + 150), e -> node.setStyle(originalStyle)));
        }
        timeline.play();
    }

    /**
     * Creates a fade-in animation for a node.
     */
    public static void fadeIn(Node node, double durationSeconds) {
        FadeTransition fade = new FadeTransition(Duration.seconds(durationSeconds), node);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    /**
     * Formats a double as currency.
     */
    public static String formatCurrency(double amount) {
        return CURRENCY_FORMAT.format(amount);
    }

    /**
     * Formats a LocalDateTime as a readable timestamp.
     */
    public static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(TIMESTAMP_FORMATTER);
    }

    /**
     * Gets the CSS color for a toast type.
     */
    private static String getToastColor(ToastType type) {
        return switch (type) {
            case SUCCESS -> "#28a745";
            case FRAUD -> "#dc3545";
            case WARNING -> "#ffc107";
            case INFO -> "#007bff";
        };
    }

    /**
     * Toast notification types.
     */
    public enum ToastType {
        SUCCESS,
        FRAUD,
        WARNING,
        INFO
    }
}

package io.github.michaelcommitsat3am.transactionfraudcomp.component.security;

import java.util.regex.Pattern;

/**
 * Centralized input validation utility to prevent injection attacks
 * and ensure data integrity across the transaction system.
 */
public final class InputValidator {

    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    private static final Pattern DEVICE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,100}$");
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");

    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;

    private static final double MIN_AMOUNT = 0.01;
    private static final double MAX_AMOUNT = 1_000_000.00;
    private static final int MAX_LOCATION_LENGTH = 200;

    private InputValidator() {
        // Prevent instantiation
    }

    /**
     * Validates user ID format and length.
     * 
     * @throws IllegalArgumentException if invalid
     */
    public static void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw new IllegalArgumentException(
                    "User ID must be 3-50 characters, alphanumeric with hyphens/underscores only");
        }
    }

    /**
     * Validates device ID format and length.
     * 
     * @throws IllegalArgumentException if invalid
     */
    public static void validateDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        if (!DEVICE_ID_PATTERN.matcher(deviceId).matches()) {
            throw new IllegalArgumentException(
                    "Device ID must be 3-100 characters, alphanumeric with hyphens/underscores only");
        }
    }

    /**
     * Validates IP address format (IPv4 only for simplicity).
     * 
     * @throws IllegalArgumentException if invalid
     */
    public static void validateIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("IP address cannot be null or empty");
        }
        if (!IP_PATTERN.matcher(ipAddress).matches()) {
            throw new IllegalArgumentException("Invalid IPv4 address format");
        }
    }

    /**
     * Validates latitude is within valid range.
     * 
     * @throws IllegalArgumentException if invalid
     */
    public static void validateLatitude(double latitude) {
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IllegalArgumentException(
                    String.format("Latitude must be between %.1f and %.1f", MIN_LATITUDE, MAX_LATITUDE));
        }
    }

    /**
     * Validates longitude is within valid range.
     * 
     * @throws IllegalArgumentException if invalid
     */
    public static void validateLongitude(double longitude) {
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException(
                    String.format("Longitude must be between %.1f and %.1f", MIN_LONGITUDE, MAX_LONGITUDE));
        }
    }

    /**
     * Validates transaction amount is positive and within reasonable bounds.
     * 
     * @throws IllegalArgumentException if invalid
     */
    public static void validateAmount(double amount) {
        if (Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Amount cannot be NaN or Infinite");
        }
        if (amount < MIN_AMOUNT) {
            throw new IllegalArgumentException(
                    String.format("Amount must be at least %.2f", MIN_AMOUNT));
        }
        if (amount > MAX_AMOUNT) {
            throw new IllegalArgumentException(
                    String.format("Amount cannot exceed %.2f", MAX_AMOUNT));
        }
    }

    /**
     * Validates and sanitizes location string.
     * 
     * @return sanitized location string
     * @throws IllegalArgumentException if invalid
     */
    public static String validateAndSanitizeLocation(String location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }

        // Remove any potential SQL injection characters
        String sanitized = location.replaceAll("[';\"\\\\]", "");

        if (sanitized.length() > MAX_LOCATION_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Location string cannot exceed %d characters", MAX_LOCATION_LENGTH));
        }

        return sanitized;
    }

    /**
     * Validates all transaction metadata at once.
     * 
     * @throws IllegalArgumentException if any validation fails
     */
    public static void validateTransactionMetadata(
            String userId,
            double amount,
            double latitude,
            double longitude,
            String deviceId,
            String ipAddress) {

        validateUserId(userId);
        validateAmount(amount);
        validateLatitude(latitude);
        validateLongitude(longitude);
        validateDeviceId(deviceId);
        validateIpAddress(ipAddress);
    }
}

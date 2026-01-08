package io.github.michaelcommitsat3am.transactionfraudcomp.component.core;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction {
    private final String transactionId;
    private final String userId;
    private final double amount;
    private final TransactionType type;
    private final LocalDateTime timestamp;
    private final String location;
    private final double latitude;
    private final double longitude;
    private final String deviceId;
    private final String ipAddress;
    private final String merchantType;

    // Use @JsonCreator to tell Jackson how to build the object without setters
    @JsonCreator
    public Transaction(
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("userId") String userId,
            @JsonProperty("amount") double amount,
            @JsonProperty("type") TransactionType type,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("location") String location,
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude,
            @JsonProperty("deviceId") String deviceId,
            @JsonProperty("ipAddress") String ipAddress,
            @JsonProperty("merchantType") String merchantType) {
        this.transactionId = transactionId != null ? transactionId : UUID.randomUUID().toString();
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.merchantType = merchantType;
    }

    // Constructor for new transactions (used by your App)
    public Transaction(String userId, double amount, TransactionType type,
                       String location, double latitude, double longitude,
                       String deviceId, String ipAddress, String merchantType) {
        this(null, userId, amount, type, null, location, latitude, longitude, deviceId, ipAddress, merchantType);
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getUserId() { return userId; }
    public double getAmount() { return amount; }
    public TransactionType getType() { return type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getLocation() { return location; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDeviceId() { return deviceId; }
    public String getIpAddress() { return ipAddress; }
    public String getMerchantType() { return merchantType; }

    @Override
    public String toString() {
        return String.format("[%s] %s: $%.2f at %s (User: %s)",
                timestamp, type, amount, location, userId);
    }
}
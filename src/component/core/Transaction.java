package component.core;

import java.time.LocalDateTime;

public class Transaction {
    private final double amount;
    private final TransactionType type;
    private final LocalDateTime timestamp;
    private final String location;
    private final double latitude;
    private final double longitude;

    public Transaction(double amount, TransactionType type, String location, double latitude, double longitude) {
        this.amount = amount;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters omitted for brevity
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getLocation() { return location; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
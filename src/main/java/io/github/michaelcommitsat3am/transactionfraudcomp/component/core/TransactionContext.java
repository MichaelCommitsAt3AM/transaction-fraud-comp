package io.github.michaelcommitsat3am.transactionfraudcomp.component.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionContext {
    private final double amount;
    private final double currentBalance;
    private final List<Transaction> history;
    // New coordinate fields for the *current* request
    private final double latitude;
    private final double longitude;
    private final String location;


    public TransactionContext(double amount, double currentBalance, List<Transaction> history, double latitude, double longitude, String location) {
        this.amount = amount;
        this.currentBalance = currentBalance;
        // Defensive copy
        this.history = new ArrayList<>(history);
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = location;
    }

    public double getAmount() { return amount; }
    public double getCurrentBalance() { return currentBalance; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getLocation() { return location; }  // <- getter


    public List<Transaction> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
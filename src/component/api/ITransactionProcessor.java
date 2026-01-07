package component.api;

import component.core.TransactionType;
import component.model.TransactionResult;

public interface ITransactionProcessor {

    /**
     * Processes a transaction with geolocation data and location string.
     * @param amount Transaction amount
     * @param type DEPOSIT or WITHDRAWAL
     * @param lat Latitude
     * @param lon Longitude
     * @param location Human-readable location (e.g., city)
     */
    TransactionResult processTransaction(double amount, TransactionType type, double lat, double lon, String location);

    double getCurrentBalance();
    void addTransactionListener(TransactionListener listener);
    void removeTransactionListener(TransactionListener listener);
}

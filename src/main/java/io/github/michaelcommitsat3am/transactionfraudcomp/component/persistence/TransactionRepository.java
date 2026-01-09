package io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {
    private final DataSource dataSource;

    public TransactionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Fetches the current balance and LOCKS the row to prevent concurrent updates.
     * Uses "SELECT ... FOR UPDATE"
     */
    public double getBalanceForUpdate(Connection conn, String userId) throws SQLException {
        String sql = "SELECT balance FROM accounts WHERE user_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                } else {
                    // Create account on the fly if it doesn't exist (or throw exception)
                    createAccount(conn, userId);
                    return 0.0;
                }
            }
        }
    }

    private void createAccount(Connection conn, String userId) throws SQLException {
        String sql = "INSERT INTO accounts (user_id, balance) VALUES (?, 0.00)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }
    }

    public void updateBalance(Connection conn, String userId, double newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, userId);
            ps.executeUpdate();
        }
    }

    public void saveTransaction(Connection conn, Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions (transaction_id, user_id, amount, timestamp, location, lat, lon, device_id, ip_address, merchant_type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, t.getTransactionId());
            ps.setString(2, t.getUserId());
            ps.setDouble(3, t.getAmount());
            ps.setTimestamp(4, Timestamp.valueOf(t.getTimestamp()));
            ps.setString(5, t.getLocation());
            ps.setDouble(6, t.getLatitude());
            ps.setDouble(7, t.getLongitude());
            ps.setString(8, t.getDeviceId());
            ps.setString(9, t.getIpAddress());
            ps.setString(10, t.getMerchantType());
            ps.executeUpdate();
        }
    }

    public List<Transaction> getHistoryByUser(String userId, int limit) {
        List<Transaction> history = new ArrayList<>();
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setInt(2, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Reconstruct object (simplified for brevity)
                    history.add(new Transaction(
                            rs.getString("user_id"),
                            rs.getDouble("amount"),
                            TransactionType.DEPOSIT, // Simplified: In real main.java.io.github.michaelcommitsat3am.transactionfraudcomp.app, store/retrieve Enum string
                            rs.getString("location"),
                            rs.getDouble("lat"),
                            rs.getDouble("lon"),
                            rs.getString("device_id"),
                            rs.getString("ip_address"),
                            rs.getString("merchant_type")
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }
}
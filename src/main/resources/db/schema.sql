-- Transaction Fraud Detection System Database Schema
-- PostgreSQL Schema with Indexes and Constraints

-- Drop existing tables if they exist
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS accounts CASCADE;

-- Accounts table with balance tracking
CREATE TABLE accounts (
    user_id VARCHAR(50) PRIMARY KEY,
    balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT balance_non_negative CHECK (balance >= 0),
    CONSTRAINT user_id_format CHECK (LENGTH(user_id) >= 3 AND LENGTH(user_id) <= 50)
);

-- Transactions table with full audit trail
CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(50) UNIQUE NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    transaction_type VARCHAR(20),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    location VARCHAR(200),
    lat DECIMAL(10, 6),
    lon DECIMAL(11, 6),
    device_id VARCHAR(100),
    ip_address VARCHAR(45), -- IPv6 compatible
    merchant_type VARCHAR(100),
    
    -- Constraints
    CONSTRAINT amount_positive CHECK (amount > 0),
    CONSTRAINT lat_valid CHECK (lat >= -90 AND lat <= 90),
    CONSTRAINT lon_valid CHECK (lon >= -180 AND lon <= 180),
    
    -- Foreign key
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES accounts(user_id) ON DELETE CASCADE
);

-- Create indexes for performance optimization
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp DESC);
CREATE INDEX idx_transactions_user_timestamp ON transactions(user_id, timestamp DESC);

-- Create updated_at trigger for accounts
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE
    ON accounts FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data for testing (optional)
INSERT INTO accounts (user_id, balance) VALUES 
    ('user_alice_001', 1000.00),
    ('user_bob_002', 500.00)
ON CONFLICT (user_id) DO NOTHING;

-- Performance analysis
COMMENT ON TABLE accounts IS 'User account balances with audit timestamps';
COMMENT ON TABLE transactions IS 'Full transaction history with geolocation and device tracking';
COMMENT ON INDEX idx_transactions_user_timestamp IS 'Composite index for user transaction history queries';

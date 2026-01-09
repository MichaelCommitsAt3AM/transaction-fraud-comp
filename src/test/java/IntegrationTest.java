package io.github.michaelcommitsat3am.transactionfraudcomp;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionEngine;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionResult;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.security.JwtService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest {

    // 1. Define Postgres Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    static TransactionRepository repo;
    static TransactionEngine engine;

    // Valid mock token
    static final String AUTH_TOKEN = "Bearer user_test_token";
    static final String TEST_USER = "user_test";

    @BeforeAll
    static void startContainers() {
        postgres.start();

        // 2. Connect Repo to Container
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setUrl(postgres.getJdbcUrl());
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());

        repo = new TransactionRepository(ds);

        // Mock Cache for this test (Redis integration can be added similarly)
        TransactionCacheManager mockCache = new TransactionCacheManager(null) {
            @Override public void addTransaction(io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction t) {}
            @Override public java.util.List<io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction> getRecentTransactions(String u) { return java.util.List.of(); }
        };

        engine = new TransactionEngine(5000.00, repo, mockCache, new JwtService());
    }

    @AfterAll
    static void stopContainers() {
        postgres.stop();
    }

    @BeforeEach
    void setupData() throws Exception {
        try (Connection conn = repo.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS transactions");
            stmt.execute("DROP TABLE IF EXISTS accounts");
            stmt.execute("CREATE TABLE accounts (user_id VARCHAR(50) PRIMARY KEY, balance DECIMAL(15, 2))");
            stmt.execute("CREATE TABLE transactions (transaction_id VARCHAR(50), user_id VARCHAR(50), amount DECIMAL, timestamp TIMESTAMP, location VARCHAR, lat DECIMAL, lon DECIMAL, device_id VARCHAR, ip_address VARCHAR, merchant_type VARCHAR)");
            stmt.execute("INSERT INTO accounts VALUES ('" + TEST_USER + "', 100.00)");
        }
    }

    @Test
    void testConcurrencyProtection() throws InterruptedException {
        // Scenario: 2 Threads try to withdraw $100 simultaneously.
        // Balance is $100. Only ONE should succeed. The other should get Insufficient Funds.

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        TransactionResult[] results = new TransactionResult[threads];

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                results[index] = engine.processTransaction(
                        AUTH_TOKEN, TEST_USER, 100.00, TransactionType.WITHDRAWAL,
                        40.71, -74.00, "dev1", "127.0.0.1"
                );
                latch.countDown();
            });
        }

        latch.await();

        int successes = 0;
        int failures = 0;
        for (TransactionResult r : results) {
            if (r.isSuccess()) successes++;
            else failures++;
        }

        assertEquals(1, successes, "Exactly one transaction should succeed due to Row Locking");
        assertEquals(1, failures, "The second transaction should fail due to Insufficient Funds");
    }
}
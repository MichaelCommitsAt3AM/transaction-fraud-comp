import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionEngine;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionResult;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.security.JwtService;
import org.junit.jupiter.api.*;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IntegrationTest {

    /* ---------- Test Constants ---------- */

    private static final String TEST_USER = "user_test";
    private static final String JWT_SECRET =
            "mySuperSecretKeyForProductionTesting123!"; // >= 256 bits
    private static final double DAILY_LIMIT = 5_000.00;

    /* ---------- Infrastructure ---------- */

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:15-alpine");

    private TransactionRepository repository;
    private TransactionEngine engine;
    private JwtService jwtService;
    private String validToken;

    /* ---------- Lifecycle ---------- */

    @BeforeAll
    void initInfrastructure() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());

        repository = new TransactionRepository(dataSource);
        jwtService = new JwtService(JWT_SECRET);
        validToken = "Bearer " + jwtService.generateToken(TEST_USER);

        TransactionCacheManager noOpCache = new NoOpTransactionCacheManager();

        engine = new TransactionEngine(
                DAILY_LIMIT,
                repository,
                noOpCache,
                jwtService
        );
    }

    @BeforeEach
    void resetDatabase() throws Exception {
        try (Connection conn = repository.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP TABLE IF EXISTS transactions");
            stmt.execute("DROP TABLE IF EXISTS accounts");

            stmt.execute("""
                CREATE TABLE accounts (
                    user_id VARCHAR(50) PRIMARY KEY,
                    balance DECIMAL(15,2)
                )
            """);

            stmt.execute("""
                CREATE TABLE transactions (
                    transaction_id VARCHAR(50),
                    user_id VARCHAR(50),
                    amount DECIMAL,
                    timestamp TIMESTAMP,
                    location VARCHAR,
                    lat DECIMAL,
                    lon DECIMAL,
                    device_id VARCHAR,
                    ip_address VARCHAR,
                    merchant_type VARCHAR
                )
            """);

            stmt.execute(
                    "INSERT INTO accounts VALUES ('" + TEST_USER + "', 100.00)"
            );
        }
    }

    /* ---------- Tests ---------- */

    @Test
    void doubleSpend_isPrevented_byDatabaseLocking() throws InterruptedException {
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        TransactionResult[] results = new TransactionResult[threads];

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    results[index] = engine.processTransaction(
                            validToken,
                            TEST_USER,
                            100.00,
                            TransactionType.WITHDRAWAL,
                            40.71,
                            -74.00,
                            "device-1",
                            "127.0.0.1"
                    );
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        long successes = List.of(results)
                .stream()
                .filter(TransactionResult::isSuccess)
                .count();

        assertEquals(
                1,
                successes,
                "Exactly one withdrawal should succeed under concurrent access"
        );
    }

    @Test
    void invalidJwt_isRejected() {
        TransactionResult result = engine.processTransaction(
                "Bearer invalid.token.value",
                TEST_USER,
                10.00,
                TransactionType.DEPOSIT,
                0,
                0,
                "device-x",
                "ip-x"
        );

        assertFalse(result.isSuccess());
        assertTrue(
                result.getMessage().toLowerCase().contains("auth"),
                "Failure must be authentication-related"
        );
    }

    /* ---------- Test Utilities ---------- */

    /**
     * Explicit no-op cache to isolate DB locking behavior.
     * Avoids anonymous classes and accidental production leakage.
     */
    private static final class NoOpTransactionCacheManager
            extends TransactionCacheManager {

        NoOpTransactionCacheManager() {
            super(null);
        }

        @Override
        public void addTransaction(Transaction transaction) {
            // intentionally no-op
        }

        @Override
        public List<Transaction> getRecentTransactions(String userId) {
            return List.of();
        }
    }
}

package io.github.michaelcommitsat3am.transactionfraudcomp.test;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.ITransactionProcessor;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionType;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.factory.TransactionEngineFactory;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionResult;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.TransactionStatus;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.rules.ImpossibleTravelRule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.rules.LargeAmountRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComponentTest {

    // Dummy Metadata Constants
    private static final String TEST_USER = "test_user_001";
    private static final String TEST_DEVICE = "device_test_01";
    private static final String TEST_IP = "127.0.0.1";

    public static void main(String[] args) {
        runTest("Test Deposit Success", testDeposit());
        runTest("Test Withdrawal Insufficient Funds", testInsufficientFunds());
        runTest("Test Large Amount Fraud", testLargeAmountFraud());
        runTest("Test Impossible Travel Fraud", testImpossibleTravel());
    }

    private static void runTest(String name, boolean passed) {
        System.out.println((passed ? "✅ PASS: " : "❌ FAIL: ") + name);
    }

    // --- Helpers for Mocking ---

    /**
     * Creates a No-Op Repository that does nothing (avoids DB connection errors).
     */
    private static TransactionRepository createMockRepo() {
        return new TransactionRepository(null) {
            @Override
            public void saveTransaction(Transaction t) {
                // No-op: Do not hit the database
            }
            @Override
            public List<Transaction> getHistoryByUser(String userId, int limit) {
                return Collections.emptyList();
            }
        };
    }

    /**
     * Creates an In-Memory Cache so that rules like Impossible Travel
     * can actually see the history of previous transactions during the test.
     */
    private static TransactionCacheManager createMockCache() {
        return new TransactionCacheManager(null) {
            private final List<Transaction> memory = new ArrayList<>();

            @Override
            public void addTransaction(Transaction t) {
                memory.add(t);
            }

            @Override
            public List<Transaction> getRecentTransactions(String userId) {
                // Return a copy of the list
                return new ArrayList<>(memory);
            }
        };
    }

    // --- Tests ---

    private static boolean testDeposit() {
        // Setup: 0 Balance, No Rules, Mocks
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(
                0,
                1000,
                createMockRepo(),
                createMockCache(),
                null,
                null
        );

        // Action: Deposit $100
        TransactionResult result = engine.processTransaction(
                TEST_USER, 100.0, TransactionType.DEPOSIT,
                40.7128, -74.0060, TEST_DEVICE, TEST_IP
        );

        // Assert
        return result.getStatus() == TransactionStatus.SUCCESS && engine.getCurrentBalance() == 100.0;
    }

    private static boolean testInsufficientFunds() {
        // Setup: $50 Balance
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(
                50,
                1000,
                createMockRepo(),
                createMockCache(),
                null,
                null
        );

        // Action: Withdraw $100 (Exceeds balance)
        TransactionResult result = engine.processTransaction(
                TEST_USER, 100.0, TransactionType.WITHDRAWAL,
                40.7128, -74.0060, TEST_DEVICE, TEST_IP
        );

        // Assert
        return result.getStatus() == TransactionStatus.DECLINED;
    }

    private static boolean testLargeAmountFraud() {
        // Setup: Rule limits single transaction to 500
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(
                1000,
                5000,
                createMockRepo(),
                createMockCache(),
                List.of(new LargeAmountRule(500)),
                null
        );

        // Action: Withdraw $600 (Exceeds rule)
        TransactionResult result = engine.processTransaction(
                TEST_USER, 600.0, TransactionType.WITHDRAWAL,
                40.7128, -74.0060, TEST_DEVICE, TEST_IP
        );

        // Assert
        return result.getStatus() == TransactionStatus.FRAUD_DETECTED;
    }

    private static boolean testImpossibleTravel() {
        // Setup: Impossible Travel Rule
        // IMPORTANT: We use the In-Memory Mock Cache so the second transaction "sees" the first one.
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(
                1000,
                5000,
                createMockRepo(),
                createMockCache(), // <--- Stores history
                List.of(new ImpossibleTravelRule()),
                null
        );

        // Action 1: Transaction in New York (Should Succeed)
        engine.processTransaction(
                TEST_USER, 100.0, TransactionType.WITHDRAWAL,
                40.7128, -74.0060, TEST_DEVICE, TEST_IP
        );

        // Action 2: Transaction in London immediately after (Should Fail)
        TransactionResult result = engine.processTransaction(
                TEST_USER, 100.0, TransactionType.WITHDRAWAL,
                51.5074, -0.1278, TEST_DEVICE, TEST_IP
        );

        // Assert
        return result.getStatus() == TransactionStatus.FRAUD_DETECTED;
    }
}
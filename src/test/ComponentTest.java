package test;

import component.api.ITransactionProcessor;
import component.core.TransactionType;
import component.factory.TransactionEngineFactory;
import component.model.TransactionResult;
import component.model.TransactionStatus;
import component.rules.ImpossibleTravelRule;
import component.rules.LargeAmountRule;

import java.util.List;

public class ComponentTest {

    public static void main(String[] args) {
        runTest("Test Deposit Success", testDeposit());
        runTest("Test Withdrawal Insufficient Funds", testInsufficientFunds());
        runTest("Test Large Amount Fraud", testLargeAmountFraud());
        runTest("Test Impossible Travel Fraud", testImpossibleTravel());
    }

    private static void runTest(String name, boolean passed) {
        System.out.println((passed ? "✅ PASS: " : "❌ FAIL: ") + name);
    }

    private static boolean testDeposit() {
        // Setup: 0 Balance, No Rules
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(0, 1000, null, null);

        // Action: Deposit $100 (Using NY Coordinates)
        TransactionResult result = engine.processTransaction(100.0, TransactionType.DEPOSIT, 40.7128, -74.0060);

        // Assert
        return result.getStatus() == TransactionStatus.SUCCESS && engine.getCurrentBalance() == 100.0;
    }

    private static boolean testInsufficientFunds() {
        // Setup: $50 Balance
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(50, 1000, null, null);

        // Action: Withdraw $100 (Exceeds balance)
        TransactionResult result = engine.processTransaction(100.0, TransactionType.WITHDRAWAL, 40.7128, -74.0060);

        // Assert
        return result.getStatus() == TransactionStatus.DECLINED;
    }

    private static boolean testLargeAmountFraud() {
        // Setup: Rule limits single transaction to 500
        ITransactionProcessor engine =
                TransactionEngineFactory.createConfiguredEngine(
                        1000,
                        5000,
                        List.of(new LargeAmountRule(500)),
                        null
                );

        // Action: Withdraw $600 (Exceeds rule)
        TransactionResult result =
                engine.processTransaction(600.0, TransactionType.WITHDRAWAL, 40.7128, -74.0060);

        // Assert
        return result.getStatus() == TransactionStatus.FRAUD_DETECTED;
    }

    private static boolean testImpossibleTravel() {
        // Setup: Impossible Travel Rule
        ITransactionProcessor engine =
                TransactionEngineFactory.createConfiguredEngine(
                        1000,
                        5000,
                        List.of(new ImpossibleTravelRule()),
                        null
                );

        // Action 1: Transaction in New York (Should Succeed)
        engine.processTransaction(100.0, TransactionType.WITHDRAWAL, 40.7128, -74.0060);

        // Action 2: Transaction in London immediately after (Should Fail)
        // Distance check via Haversine will flag this speed as impossible
        TransactionResult result =
                engine.processTransaction(100.0, TransactionType.WITHDRAWAL, 51.5074, -0.1278);

        // Assert
        return result.getStatus() == TransactionStatus.FRAUD_DETECTED;
    }
}
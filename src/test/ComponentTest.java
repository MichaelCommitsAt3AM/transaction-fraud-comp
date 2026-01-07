package test;

import component.api.ITransactionProcessor;
import component.core.TransactionType;
import component.factory.TransactionEngineFactory;
import component.model.TransactionResult;
import component.model.TransactionStatus;
import component.rules.LargeAmountRule;

import java.util.List;

public class ComponentTest {

    public static void main(String[] args) {
        runTest("Test Deposit Success", testDeposit());
        runTest("Test Withdrawal Insufficient Funds", testInsufficientFunds());
        runTest("Test Fraud Detection", testFraudDetection());
    }

    private static void runTest(String name, boolean passed) {
        System.out.println((passed ? "✅ PASS: " : "❌ FAIL: ") + name);
    }

    private static boolean testDeposit() {
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(0, 1000, null, null);
        TransactionResult result = engine.processTransaction(100.0, TransactionType.DEPOSIT);
        return result.getStatus() == TransactionStatus.SUCCESS && engine.getCurrentBalance() == 100.0;
    }

    private static boolean testInsufficientFunds() {
        ITransactionProcessor engine = TransactionEngineFactory.createConfiguredEngine(50, 1000, null, null);
        TransactionResult result = engine.processTransaction(100.0, TransactionType.WITHDRAWAL);
        return result.getStatus() == TransactionStatus.DECLINED;
    }

    private static boolean testFraudDetection() {
        ITransactionProcessor engine =
                TransactionEngineFactory.createConfiguredEngine(
                        1000,
                        5000,
                        List.of(new LargeAmountRule(500)),
                        null
                );

        TransactionResult result =
                engine.processTransaction(600.0, TransactionType.WITHDRAWAL);

        return result.getStatus() == TransactionStatus.FRAUD_DETECTED;
    }
}
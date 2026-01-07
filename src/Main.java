import app.banking.BankingApplication;
import app.atm.ATMApplication;

public class Main {
    public static void main(String[] args) {

        // ==========================================
        // SCENARIO 1: The Banking App (High Limits)
        // ==========================================
        System.out.println("==========================================");
        System.out.println("      TESTING BANKING APPLICATION");
        System.out.println("==========================================");

        BankingApplication bankApp = new BankingApplication();

        // 1. Success Scenario
        bankApp.deposit(500);  // Balance -> 1500

        // 2. Insufficient Funds
        bankApp.withdraw(5000); // Fail

        // 3. Fraud Scenario (Large Amount Rule > 2000)
        bankApp.withdraw(2500); // Fail (Fraud)

        // 4. Invalid Input
        bankApp.deposit(-100);  // Fail (Invalid)


        // ==========================================
        // SCENARIO 2: The ATM App (Reuse Proof)
        // ==========================================
        System.out.println("\n\n==========================================");
        System.out.println("      TESTING ATM COMPONENT REUSE");
        System.out.println("==========================================");

        ATMApplication atm = new ATMApplication();

        // 1. Success (Small amount)
        atm.withdrawCash(50);

        // 2. Fraud (ATM Rule: > 200 is fraud)
        // Note: In the Bank app, $300 would be fine. In ATM, it is fraud.
        // This proves the component is configurable!
        atm.withdrawCash(300);
    }
}
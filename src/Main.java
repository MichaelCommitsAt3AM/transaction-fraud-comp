import app.banking.BankingApplication;
import app.atm.ATMApplication;

public class Main {

    // --- Coordinate Constants ---
    private static final double NY_LAT = 40.7128;
    private static final double NY_LON = -74.0060;

    private static final double LONDON_LAT = 51.5074;
    private static final double LONDON_LON = -0.1278;

    private static final double CHI_LAT = 41.8781;
    private static final double CHI_LON = -87.6298;

    public static void main(String[] args) {

        // ==========================================
        // SCENARIO 1: The Banking App
        // Features: High Limits, Location Awareness, Anomaly Detection
        // ==========================================
        System.out.println("==========================================");
        System.out.println("      TESTING BANKING APPLICATION");
        System.out.println("==========================================");

        BankingApplication bankApp = new BankingApplication();

        // --- PART A: Standard Logic (Original Scenarios) ---

        // 1. Success Scenario (Balance: 1000 -> 1500)
        bankApp.deposit(500, NY_LAT, NY_LON);

        // 2. Insufficient Funds (5000 > 1500)
        bankApp.withdraw(5000, NY_LAT, NY_LON); // Fail

        // 3. Fraud Scenario (Large Amount Rule > 2000)
        bankApp.withdraw(2500, NY_LAT, NY_LON); // Fail (Fraud)

        // 4. Invalid Input
        bankApp.deposit(-100, NY_LAT, NY_LON);  // Fail (Invalid)


        // --- PART B: Smart Logic (New Scenarios) ---
        System.out.println("\n--- Testing Smart Fraud Rules ---");

        // 5. Establish Baseline History
        // Current History: [Deposit 500]. Balance: 1500.
        // We add a small withdrawal to set a normal "Average Spending"
        bankApp.withdraw(50, NY_LAT, NY_LON);
        // History: [Dep 500, Wd 50]. Avg ~275.
        // 3x Spending Anomaly Threshold ~825.

        // 6. IMPOSSIBLE TRAVEL Rule
        // Previous Tx was in "New York". Now trying "London" instantly.
        // Distance ~5570km in 0 seconds -> Infinite Speed.
        bankApp.withdraw(100, LONDON_LAT, LONDON_LON); // Fail (Impossible Travel)

        // 7. SPENDING ANOMALY Rule
        // User tries to withdraw 1200.
        // 1200 is < 2000 (Standard Large Amount Limit), so it usually passes.
        // BUT 1200 is > 825 (3x User Average), so it is flagged as anomalous.
        bankApp.withdraw(1200, NY_LAT, NY_LON); // Fail (Anomaly)


        // ==========================================
        // SCENARIO 2: The ATM App
        // Features: Low Limits, Reuse, Card Testing Detection
        // ==========================================
        System.out.println("\n\n==========================================");
        System.out.println("      TESTING ATM COMPONENT REUSE");
        System.out.println("==========================================");

        ATMApplication atm = new ATMApplication();

        // --- PART A: Standard Logic ---

        // 1. Success (Small amount)
        atm.withdrawCash(50, CHI_LAT, CHI_LON);

        // 2. Fraud (ATM Rule: > 200 is fraud)
        // Note: In the Bank app, $300 would be fine. In ATM, it is fraud.
        atm.withdrawCash(300, CHI_LAT, CHI_LON);


        // --- PART B: Smart Logic ---
        System.out.println("\n--- Testing ATM Specific Rules ---");

        // 3. CARD TESTING Rule
        // Fraudsters often test a card with a $1 charge before a larger theft.

        // Step 1: The "Test"
        atm.withdrawCash(1.00, CHI_LAT, CHI_LON); // Success

        // Step 2: The "Drain"
        // Trying to withdraw $150 immediately after $1.
        // Note: $150 is allowed by the $200 limit, but flagged by the Pattern Detector.
        atm.withdrawCash(150.00, CHI_LAT, CHI_LON); // Fail (Card Testing Pattern)
    }
}
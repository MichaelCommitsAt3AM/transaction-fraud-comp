package io.github.michaelcommitsat3am.transactionfraudcomp;

import io.github.michaelcommitsat3am.transactionfraudcomp.app.banking.BankingApplication;
import io.github.michaelcommitsat3am.transactionfraudcomp.app.atm.ATMApplication;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {

    // --- Coordinate Constants ---
    private static final double NY_LAT = 40.7128;
    private static final double NY_LON = -74.0060;
    private static final double LONDON_LAT = 51.5074;
    private static final double LONDON_LON = -0.1278;
    private static final double CHI_LAT = 41.8781;
    private static final double CHI_LON = -87.6298;

    // --- Metadata Constants ---
    private static final String USER_ALICE = "user_alice_001";
    private static final String USER_BOB = "user_bob_002";
    private static final String IPHONE_12 = "device_iphone12_x88";
    private static final String ATM_DEVICE = "device_atm_kiosk_04";
    private static final String IP_HOME = "192.168.1.50";
    private static final String IP_CELL = "10.0.0.1";

    public static void main(String[] args) {

        System.out.println("--- 1. Loading Configuration ---");
        Properties config = loadConfiguration();

        System.out.println("--- 2. Initializing Connections ---");

        // ==========================================
        // 0. INFRASTRUCTURE SETUP (REAL DB)
        // ==========================================

        // 1. Setup PostgreSQL Data Source
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{config.getProperty("db.host")});
        dataSource.setPortNumbers(new int[]{Integer.parseInt(config.getProperty("db.port"))});
        dataSource.setDatabaseName(config.getProperty("db.name"));
        dataSource.setUser(config.getProperty("db.user"));
        dataSource.setPassword(config.getProperty("db.password"));

        // 2. Setup Redis Client (Lettuce)
        RedisClient redisClient = RedisClient.create(config.getProperty("redis.uri"));
        StatefulRedisConnection<String, String> redisConnection = redisClient.connect();

        // 3. Create Repositories with REAL connections
        TransactionRepository repo = new TransactionRepository(dataSource);
        TransactionCacheManager cache = new TransactionCacheManager(redisConnection);

        System.out.println("✅ Infrastructure Initialized (PostgreSQL + Redis Connected) ---");


        // ==========================================
        // SCENARIO 1: The Banking App
        // Features: High Limits, Location Awareness, Anomaly Detection
        // ==========================================
        System.out.println("\n==========================================");
        System.out.println("      TESTING BANKING APPLICATION");
        System.out.println("==========================================");

        BankingApplication bankApp = new BankingApplication(repo, cache);

        // --- PART A: Standard Logic ---
        // 1. Success Scenario
        bankApp.deposit(USER_ALICE, 500, NY_LAT, NY_LON, IPHONE_12, IP_HOME);

        // 2. Insufficient Funds
        bankApp.withdraw(USER_ALICE, 5000, NY_LAT, NY_LON, IPHONE_12, IP_HOME);

        // 3. Fraud Scenario (Large Amount)
        bankApp.withdraw(USER_ALICE, 2500, NY_LAT, NY_LON, IPHONE_12, IP_HOME);

        // 4. Invalid Input
        bankApp.deposit(USER_ALICE, -100, NY_LAT, NY_LON, IPHONE_12, IP_HOME);


        // --- PART B: Smart Logic ---
        System.out.println("\n--- Testing Smart Fraud Rules (Persisted History) ---");

        // 5. Establish Baseline History
        bankApp.withdraw(USER_ALICE, 50, NY_LAT, NY_LON, IPHONE_12, IP_HOME);

        // 6. IMPOSSIBLE TRAVEL Rule
        bankApp.withdraw(USER_ALICE, 100, LONDON_LAT, LONDON_LON, IPHONE_12, IP_CELL);

        // 7. SPENDING ANOMALY Rule
        bankApp.withdraw(USER_ALICE, 1200, NY_LAT, NY_LON, IPHONE_12, IP_HOME);


        // ==========================================
        // SCENARIO 2: The ATM App
        // Features: Low Limits, Reuse, Card Testing Detection
        // ==========================================
        System.out.println("\n\n==========================================");
        System.out.println("      TESTING ATM COMPONENT REUSE");
        System.out.println("==========================================");

        ATMApplication atm = new ATMApplication(repo, cache);

        // --- PART A: Standard Logic ---
        atm.withdrawCash(USER_BOB, 50, CHI_LAT, CHI_LON, ATM_DEVICE, IP_CELL);
        atm.withdrawCash(USER_BOB, 300, CHI_LAT, CHI_LON, ATM_DEVICE, IP_CELL); // Fraud in ATM context

        // --- PART B: Smart Logic ---
        System.out.println("\n--- Testing ATM Specific Rules ---");

        // 3. CARD TESTING Rule
        atm.withdrawCash(USER_BOB, 1.00, CHI_LAT, CHI_LON, ATM_DEVICE, IP_CELL); // Success
        atm.withdrawCash(USER_BOB, 150.00, CHI_LAT, CHI_LON, ATM_DEVICE, IP_CELL); // Fail (Card Testing Pattern)

        // --- Cleanup ---
        redisConnection.close();
        redisClient.shutdown();
    }

    /**
     * Helper to load config.properties from the classpath.
     */
    private static Properties loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.err.println("❌ ERROR: config.properties not found in classpath (src/main/resources).");
                System.exit(1);
            }
            props.load(input);
        } catch (IOException e) {
            System.err.println("❌ ERROR: Failed to load config.properties");
            e.printStackTrace();
            System.exit(1);
        }
        return props;
    }
}
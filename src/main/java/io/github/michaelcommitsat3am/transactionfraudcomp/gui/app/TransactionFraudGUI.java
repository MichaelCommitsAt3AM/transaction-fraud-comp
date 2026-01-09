package io.github.michaelcommitsat3am.transactionfraudcomp.gui.app;

import io.github.michaelcommitsat3am.transactionfraudcomp.app.banking.BankingEventHandler;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.ITransactionProcessor;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.factory.TransactionEngineFactory;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.monitoring.TransactionMetrics;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.ConnectionPoolConfig;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionCacheManager;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.TransactionRepository;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.rules.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller.DashboardController;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller.HistoryController;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller.MainController;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.controller.MetricsController;
import io.github.michaelcommitsat3am.transactionfraudcomp.gui.util.FXMLLoaderFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Main JavaFX Application for Transaction Fraud Component GUI.
 */
public class TransactionFraudGUI extends Application {

    private DataSource dataSource;
    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> redisConnection;
    private TransactionMetrics metrics;
    private ITransactionProcessor processor;
    private TransactionRepository repository;

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Transaction Fraud Detection System");

        // Initialize backend
        initializeBackend();

        // Create controllers with dependency injection
        DashboardController dashboardController = new DashboardController(processor);
        dashboardController.setStage(primaryStage);

        MetricsController metricsController = new MetricsController(metrics);
        HistoryController historyController = new HistoryController(repository);

        // Load views
        FXMLLoaderFactory.LoadResult<DashboardController> dashboardResult = FXMLLoaderFactory
                .load("/fxml/dashboard.fxml", clazz -> {
                    if (clazz == DashboardController.class)
                        return dashboardController;
                    return null;
                });

        FXMLLoaderFactory.LoadResult<MetricsController> metricsResult = FXMLLoaderFactory.load("/fxml/metrics.fxml",
                clazz -> {
                    if (clazz == MetricsController.class)
                        return metricsController;
                    return null;
                });

        FXMLLoaderFactory.LoadResult<HistoryController> historyResult = FXMLLoaderFactory.load("/fxml/history.fxml",
                clazz -> {
                    if (clazz == HistoryController.class)
                        return historyController;
                    return null;
                });

        // Load main layout
        MainController mainController = new MainController();
        FXMLLoaderFactory.LoadResult<MainController> mainResult = FXMLLoaderFactory.load("/fxml/main.fxml", clazz -> {
            if (clazz == MainController.class)
                return mainController;
            return null;
        });

        // Inject views into main controller
        mainController.setViews(
                dashboardResult.getRoot(), dashboardController,
                metricsResult.getRoot(), metricsController,
                historyResult.getRoot(), historyController);

        // Set system status
        mainController.setSystemStatus("System Ready", true);

        // Create scene
        Scene scene = new Scene(mainResult.getRoot(), 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.show();

        // Add shutdown hook
        primaryStage.setOnCloseRequest(event -> shutdown());
    }

    /**
     * Initializes the backend infrastructure.
     */
    private void initializeBackend() throws Exception {
        System.out.println("===========================================");
        System.out.println("  TRANSACTION FRAUD DETECTION GUI");
        System.out.println("  Initializing Backend...");
        System.out.println("===========================================\n");

        // Load configuration
        Properties config = loadConfiguration();

        // Setup HikariCP Connection Pool
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s",
                config.getProperty("db.host"),
                config.getProperty("db.port"),
                config.getProperty("db.name"));

        dataSource = ConnectionPoolConfig.createDataSource(
                jdbcUrl,
                config.getProperty("db.user"),
                config.getProperty("db.password"),
                "TransactionGUIPool");
        System.out.println("✅ Database connection pool initialized");

        // Setup Redis
        redisClient = RedisClient.create(config.getProperty("redis.uri"));
        redisConnection = redisClient.connect();
        System.out.println("✅ Redis connection established");

        // Create Metrics
        metrics = new TransactionMetrics();
        System.out.println("✅ Metrics initialized");

        // Create Repositories
        repository = new TransactionRepository(dataSource);
        TransactionCacheManager cache = new TransactionCacheManager(redisConnection, metrics);
        System.out.println("✅ Repositories created");

        // Create Transaction Engine with all fraud rules
        processor = TransactionEngineFactory.createConfiguredEngine(
                1000.00, // Initial Balance
                5000.00, // Daily Limit
                repository,
                cache,
                List.of(
                        new LargeAmountRule(2000.00),
                        new DailyLimitRule(5000.00),
                        new VelocityRule(3, 60),
                        new ImpossibleTravelRule(),
                        new SpendingAnomalyRule(3.0),
                        new CardTestingRule()),
                List.of(new BankingEventHandler()));
        System.out.println("✅ Transaction Engine initialized with fraud rules");
        System.out.println("\n===========================================");
        System.out.println("  Backend Initialization Complete!");
        System.out.println("===========================================\n");
    }

    /**
     * Loads configuration from config.properties.
     */
    private Properties loadConfiguration() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new IOException("config.properties not found in classpath");
            }
            props.load(input);
        }
        return props;
    }

    /**
     * Graceful shutdown of resources.
     */
    private void shutdown() {
        System.out.println("\n===========================================");
        System.out.println("  Shutting Down...");
        System.out.println("===========================================");

        if (redisConnection != null && redisConnection.isOpen()) {
            redisConnection.close();
            System.out.println("✅ Redis connection closed");
        }

        if (redisClient != null) {
            redisClient.shutdown();
            System.out.println("✅ Redis client shutdown");
        }

        if (dataSource != null) {
            ConnectionPoolConfig.closeDataSource(dataSource);
        }

        System.out.println("✅ Shutdown complete");
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package io.github.michaelcommitsat3am.transactionfraudcomp.component.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Centralized metrics tracking for transaction processing.
 * Uses Micrometer for vendor-neutral metrics collection.
 */
public class TransactionMetrics {

    private static final Logger logger = LoggerFactory.getLogger(TransactionMetrics.class);

    private final MeterRegistry registry;

    // Transaction counters
    private final Counter transactionSuccessCounter;
    private final Counter transactionDeclinedCounter;
    private final Counter transactionFraudCounter;

    // Fraud rule counters
    private final Counter fraudRuleHitCounter;

    // Database operation counters
    private final Counter databaseQueryCounter;
    private final Counter databaseErrorCounter;

    // Cache operation counters
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheErrorCounter;

    // Timers
    private final Timer transactionProcessingTimer;
    private final Timer databaseQueryTimer;
    private final Timer cacheOperationTimer;

    /**
     * Creates metrics with a simple in-memory registry.
     * In production, use a registry that exports to monitoring systems.
     */
    public TransactionMetrics() {
        this(new SimpleMeterRegistry());
    }

    /**
     * Creates metrics with a custom registry.
     */
    public TransactionMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Initialize counters
        this.transactionSuccessCounter = Counter.builder("transactions.success")
                .description("Number of successful transactions")
                .register(registry);

        this.transactionDeclinedCounter = Counter.builder("transactions.declined")
                .description("Number of declined transactions")
                .register(registry);

        this.transactionFraudCounter = Counter.builder("transactions.fraud")
                .description("Number of fraud-detected transactions")
                .register(registry);

        this.fraudRuleHitCounter = Counter.builder("fraud.rules.hit")
                .description("Number of fraud rule hits")
                .register(registry);

        this.databaseQueryCounter = Counter.builder("database.queries")
                .description("Number of database queries executed")
                .register(registry);

        this.databaseErrorCounter = Counter.builder("database.errors")
                .description("Number of database errors")
                .register(registry);

        this.cacheHitCounter = Counter.builder("cache.hits")
                .description("Number of cache hits")
                .register(registry);

        this.cacheMissCounter = Counter.builder("cache.misses")
                .description("Number of cache misses")
                .register(registry);

        this.cacheErrorCounter = Counter.builder("cache.errors")
                .description("Number of cache errors")
                .register(registry);

        // Initialize timers
        this.transactionProcessingTimer = Timer.builder("transactions.processing")
                .description("Transaction processing time")
                .register(registry);

        this.databaseQueryTimer = Timer.builder("database.query.time")
                .description("Database query execution time")
                .register(registry);

        this.cacheOperationTimer = Timer.builder("cache.operation.time")
                .description("Cache operation execution time")
                .register(registry);

        logger.info("✅ Transaction metrics initialized");
    }

    // Transaction metrics
    public void recordTransactionSuccess() {
        transactionSuccessCounter.increment();
    }

    public void recordTransactionDeclined() {
        transactionDeclinedCounter.increment();
    }

    public void recordTransactionFraud() {
        transactionFraudCounter.increment();
    }

    public void recordFraudRuleHit(String ruleName) {
        fraudRuleHitCounter.increment();
        logger.debug("Fraud rule hit: {}", ruleName);
    }

    // Database metrics
    public void recordDatabaseQuery() {
        databaseQueryCounter.increment();
    }

    public void recordDatabaseError() {
        databaseErrorCounter.increment();
    }

    public void recordDatabaseQueryTime(long durationMs) {
        databaseQueryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // Cache metrics
    public void recordCacheHit() {
        cacheHitCounter.increment();
    }

    public void recordCacheMiss() {
        cacheMissCounter.increment();
    }

    public void recordCacheError() {
        cacheErrorCounter.increment();
    }

    public void recordCacheOperationTime(long durationMs) {
        cacheOperationTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    // Transaction processing timer
    public Timer.Sample startTransactionTimer() {
        return Timer.start(registry);
    }

    public void stopTransactionTimer(Timer.Sample sample) {
        sample.stop(transactionProcessingTimer);
    }

    /**
     * Gets the meter registry for custom metrics or integration.
     */
    public MeterRegistry getRegistry() {
        return registry;
    }

    /**
     * Prints current metrics snapshot to logs.
     */
    public void logMetricsSnapshot() {
        logger.info("=== Transaction Metrics Snapshot ===");
        logger.info("Success: {}", transactionSuccessCounter.count());
        logger.info("Declined: {}", transactionDeclinedCounter.count());
        logger.info("Fraud: {}", transactionFraudCounter.count());
        logger.info("Fraud Rule Hits: {}", fraudRuleHitCounter.count());
        logger.info("DB Queries: {}", databaseQueryCounter.count());
        logger.info("DB Errors: {}", databaseErrorCounter.count());
        logger.info("Cache Hits: {}", cacheHitCounter.count());
        logger.info("Cache Misses: {}", cacheMissCounter.count());
        logger.info("Cache Errors: {}", cacheErrorCounter.count());
        logger.info("Avg Transaction Time: {} ms", transactionProcessingTimer.mean(TimeUnit.MILLISECONDS));
        logger.info("===================================");
    }
}

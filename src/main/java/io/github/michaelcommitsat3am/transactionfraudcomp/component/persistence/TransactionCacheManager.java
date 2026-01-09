package io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.Transaction;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.monitoring.TransactionMetrics;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.resilience.ResilientCircuitBreaker;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Enhanced cache manager with circuit breaker protection and graceful
 * degradation.
 */
public class TransactionCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCacheManager.class);

    private final StatefulRedisConnection<String, String> redisConnection;
    private final ObjectMapper objectMapper;
    private final ResilientCircuitBreaker circuitBreaker;
    private final TransactionMetrics metrics;

    private static final int HISTORY_WINDOW_SECONDS = 86400; // 24 hours

    public TransactionCacheManager(StatefulRedisConnection<String, String> redisConnection) {
        this(redisConnection, null);
    }

    public TransactionCacheManager(
            StatefulRedisConnection<String, String> redisConnection,
            TransactionMetrics metrics) {
        this.redisConnection = redisConnection;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // Initialize circuit breaker for Redis operations
        this.circuitBreaker = new ResilientCircuitBreaker("redis-cache", 60.0f,
                java.time.Duration.ofSeconds(30), 10);

        this.metrics = metrics != null ? metrics : new TransactionMetrics();

        logger.info("✅ TransactionCacheManager initialized with circuit breaker protection");
    }

    /**
     * Adds a transaction to the cache with circuit breaker protection.
     * Fails gracefully if Redis is unavailable.
     */
    public void addTransaction(Transaction t) {
        long startTime = System.currentTimeMillis();

        try {
            circuitBreaker.executeRunnable(() -> {
                try {
                    RedisCommands<String, String> syncCommands = redisConnection.sync();

                    String key = "history:" + t.getUserId();
                    long score = t.getTimestamp().toEpochSecond(ZoneOffset.UTC);

                    String json = serialize(t);
                    if (json != null) {
                        syncCommands.zadd(key, score, json);
                        syncCommands.expire(key, HISTORY_WINDOW_SECONDS);
                        logger.debug("Transaction cached for user: {}", t.getUserId());
                    }
                } catch (Exception e) {
                    logger.error("Redis operation failed during addTransaction", e);
                    metrics.recordCacheError();
                    throw e; // Propagate to circuit breaker
                }
            });

            metrics.recordCacheOperationTime(System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            logger.warn("Failed to cache transaction for user {}: {} (Circuit breaker may be open)",
                    t.getUserId(), e.getMessage());
            metrics.recordCacheError();
            // Graceful degradation: Don't fail the transaction if cache is down
        }
    }

    /**
     * Retrieves recent transactions with circuit breaker protection.
     * Returns empty list if Redis is unavailable (graceful degradation).
     */
    public List<Transaction> getRecentTransactions(String userId) {
        long startTime = System.currentTimeMillis();

        try {
            List<Transaction> result = circuitBreaker.executeSupplier(() -> {
                try {
                    RedisCommands<String, String> syncCommands = redisConnection.sync();

                    String key = "history:" + userId;
                    List<String> jsonList = syncCommands.zrange(key, 0, -1);

                    if (jsonList != null && !jsonList.isEmpty()) {
                        metrics.recordCacheHit();
                        logger.debug("Cache hit for user: {} ({} transactions)", userId, jsonList.size());

                        return jsonList.stream()
                                .map(this::deserialize)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    } else {
                        metrics.recordCacheMiss();
                        logger.debug("Cache miss for user: {}", userId);
                        return Collections.emptyList();
                    }
                } catch (Exception e) {
                    logger.error("Redis operation failed during getRecentTransactions", e);
                    metrics.recordCacheError();
                    throw e; // Propagate to circuit breaker
                }
            });

            metrics.recordCacheOperationTime(System.currentTimeMillis() - startTime);
            return result;

        } catch (Exception e) {
            logger.warn("Failed to retrieve cached transactions for user {}: {} (Circuit breaker may be open)",
                    userId, e.getMessage());
            metrics.recordCacheError();
            metrics.recordCacheMiss();

            // Graceful degradation: Return empty list instead of failing
            // Fraud rules will work with limited or no history
            return Collections.emptyList();
        }
    }

    /**
     * Serializes a transaction to JSON.
     */
    private String serialize(Transaction t) {
        try {
            return objectMapper.writeValueAsString(t);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize transaction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Deserializes a transaction from JSON.
     */
    private Transaction deserialize(String json) {
        try {
            return objectMapper.readValue(json, Transaction.class);
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing transaction: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the current circuit breaker state.
     */
    public io.github.resilience4j.circuitbreaker.CircuitBreaker.State getCircuitBreakerState() {
        return circuitBreaker.getState();
    }

    /**
     * Health check for Redis connection.
     */
    public boolean isHealthy() {
        try {
            circuitBreaker.executeSupplier(() -> {
                RedisCommands<String, String> syncCommands = redisConnection.sync();
                syncCommands.ping();
                return true;
            });
            return true;
        } catch (Exception e) {
            logger.warn("Redis health check failed: {}", e.getMessage());
            return false;
        }
    }

}
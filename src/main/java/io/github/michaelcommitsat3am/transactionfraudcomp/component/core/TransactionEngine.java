package io.github.michaelcommitsat3am.transactionfraudcomp.component.core;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.api.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.events.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.exceptions.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.model.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.monitoring.TransactionMetrics;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.persistence.*;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.security.AuthService;
import io.github.michaelcommitsat3am.transactionfraudcomp.component.security.InputValidator;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Transaction Engine with comprehensive security hardening,
 * input validation, rate limiting, and performance monitoring.
 */
public class TransactionEngine implements ITransactionProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TransactionEngine.class);

    private final AuthService authService;
    private final double dailyLimit;
    private final TransactionRepository repository;
    private final TransactionCacheManager cacheManager;
    private final TransactionMetrics metrics;
    private final List<IFraudRule> fraudRules = new ArrayList<>();
    private final List<TransactionListener> listeners = new ArrayList<>();

    // Rate limiting: Track transaction attempts per user
    private final Map<String, RateLimitTracker> rateLimitMap = new ConcurrentHashMap<>();
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final long RATE_LIMIT_WINDOW_MS = 60000; // 1 minute

    // Failed auth attempts tracking
    private final Map<String, FailedAuthTracker> failedAuthMap = new ConcurrentHashMap<>();
    private static final int MAX_FAILED_AUTH_ATTEMPTS = 5;
    private static final long AUTH_COOLDOWN_MS = 300000; // 5 minutes

    public TransactionEngine(
            double dailyLimit,
            TransactionRepository repo,
            TransactionCacheManager cache,
            AuthService authService) {
        this(dailyLimit, repo, cache, authService, null);
    }

    public TransactionEngine(
            double dailyLimit,
            TransactionRepository repo,
            TransactionCacheManager cache,
            AuthService authService,
            TransactionMetrics metrics) {
        this.dailyLimit = dailyLimit;
        this.repository = repo;
        this.cacheManager = cache;
        this.authService = authService;
        this.metrics = metrics != null ? metrics : new TransactionMetrics();

        logger.info("✅ TransactionEngine initialized with enhanced security and monitoring");
    }

    public void addFraudRule(IFraudRule rule) {
        this.fraudRules.add(rule);
        logger.debug("Fraud rule added: {}", rule.getRuleName());
    }

    public void addTransactionListener(TransactionListener listener) {
        this.listeners.add(listener);
    }

    public void removeTransactionListener(TransactionListener listener) {
        this.listeners.remove(listener);
    }

    @Override
    public double getCurrentBalance() {
        return 0.0;
    }

    public double getDailyLimit() {
        return dailyLimit;
    }

    @Override
    public TransactionResult processTransaction(
            String authToken,
            String userId,
            double amount,
            TransactionType type,
            double lat,
            double lon,
            String deviceId,
            String ip) {

        Timer.Sample sample = metrics.startTransactionTimer();
        String correlationId = UUID.randomUUID().toString().substring(0, 8);

        logger.info("[{}] Processing transaction for user: {}, amount: {}, type: {}",
                correlationId, userId, amount, type);

        try {
            // 0. INPUT VALIDATION
            try {
                InputValidator.validateTransactionMetadata(userId, amount, lat, lon, deviceId, ip);
            } catch (IllegalArgumentException e) {
                logger.warn("[{}] Input validation failed: {}", correlationId, e.getMessage());
                metrics.recordTransactionDeclined();
                return new TransactionResult(TransactionStatus.DECLINED,
                        "Invalid input: " + e.getMessage(), 0.0);
            }

            // 1. RATE LIMITING CHECK
            if (isRateLimited(userId)) {
                logger.warn("[{}] Rate limit exceeded for user: {}", correlationId, userId);
                metrics.recordTransactionDeclined();
                return new TransactionResult(TransactionStatus.DECLINED,
                        "Rate limit exceeded. Please try again later.", 0.0);
            }

            // 2. FAILED AUTH COOLDOWN CHECK
            if (isInAuthCooldown(userId)) {
                logger.warn("[{}] User {} in auth cooldown period", correlationId, userId);
                metrics.recordTransactionDeclined();
                return new TransactionResult(TransactionStatus.DECLINED,
                        "Account temporarily locked due to failed authentication attempts", 0.0);
            }

            // 3. SECURITY CHECK
            try {
                authService.validateRequest(authToken, userId);
                // Reset failed auth on success
                failedAuthMap.remove(userId);
            } catch (SecurityException e) {
                recordFailedAuth(userId);
                logger.warn("[{}] Security blocked request for user {}: {}",
                        correlationId, userId, e.getMessage());
                metrics.recordTransactionDeclined();
                return new TransactionResult(TransactionStatus.DECLINED,
                        "Auth Failed: " + e.getMessage(), 0.0);
            }

            // 4. TRANSACTION PROCESSING
            return processTransactionInternal(userId, amount, type, lat, lon, deviceId, ip, correlationId);

        } finally {
            metrics.stopTransactionTimer(sample);
        }
    }

    /**
     * Internal transaction processing with database operations.
     */
    private TransactionResult processTransactionInternal(
            String userId,
            double amount,
            TransactionType type,
            double lat,
            double lon,
            String deviceId,
            String ip,
            String correlationId) {

        String location = String.format("%.4f, %.4f", lat, lon);
        Transaction tx = new Transaction(userId, amount, type, location, lat, lon, deviceId, ip, "Standard");
        Connection conn = null;
        int retryCount = 0;
        int maxRetries = 2;

        while (retryCount <= maxRetries) {
            try {
                conn = repository.getConnection();
                conn.setAutoCommit(false);
                metrics.recordDatabaseQuery();

                // LOCK ROW & FETCH BALANCE
                long dbStartTime = System.currentTimeMillis();
                double currentBalance = repository.getBalanceForUpdate(conn, userId);
                metrics.recordDatabaseQueryTime(System.currentTimeMillis() - dbStartTime);

                // BALANCE CHECK
                if (type == TransactionType.WITHDRAWAL && amount > currentBalance) {
                    logger.info("[{}] Insufficient funds: User {} has {}, tried {}",
                            correlationId, userId, currentBalance, amount);
                    throw new InsufficientBalanceException("Insufficient funds");
                }

                // FRAUD CHECK
                List<Transaction> recentHistory = cacheManager.getRecentTransactions(userId);
                TransactionContext context = new TransactionContext(
                        amount, currentBalance, recentHistory, lat, lon, location);
                checkFraud(context, correlationId);

                // EXECUTE & COMMIT
                double newBalance = (type == TransactionType.DEPOSIT)
                        ? currentBalance + amount
                        : currentBalance - amount;

                repository.updateBalance(conn, userId, newBalance);
                repository.saveTransaction(conn, tx);
                metrics.recordDatabaseQuery();

                conn.commit();
                logger.info("[{}] Transaction approved. User: {}, Amount: {}, New Balance: {}",
                        correlationId, userId, amount, newBalance);

                // ASYNC TASKS
                try {
                    cacheManager.addTransaction(tx);
                } catch (Exception e) {
                    logger.warn("[{}] Redis update failed (non-critical)", correlationId, e);
                }

                listeners.forEach(l -> l.onApproved(
                        new TransactionApprovedEvent(amount, type, newBalance)));

                metrics.recordTransactionSuccess();
                return new TransactionResult(TransactionStatus.SUCCESS, "Approved", newBalance);

            } catch (SQLException e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                        logger.debug("[{}] Transaction rolled back", correlationId);
                    } catch (SQLException ex) {
                        logger.error("[{}] Rollback failed", correlationId, ex);
                    }
                }

                // Check if it's a deadlock or lock timeout - retry
                if (isRetryableException(e) && retryCount < maxRetries) {
                    retryCount++;
                    logger.warn("[{}] Retryable DB error, attempt {}/{}: {}",
                            correlationId, retryCount, maxRetries, e.getMessage());
                    metrics.recordDatabaseError();

                    try {
                        Thread.sleep(100 * retryCount); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    continue; // Retry
                }

                logger.error("[{}] Database error after {} retries", correlationId, retryCount, e);
                metrics.recordDatabaseError();
                metrics.recordTransactionDeclined();
                handleException(e, amount);
                return new TransactionResult(TransactionStatus.DECLINED,
                        "Database error: " + e.getMessage(), 0.0);

            } catch (Exception e) {
                if (conn != null) {
                    try {
                        conn.rollback();
                    } catch (SQLException ex) {
                        logger.error("[{}] Rollback failed", correlationId, ex);
                    }
                }

                handleException(e, amount);

                if (e instanceof FraudDetectedException) {
                    logger.warn("[{}] FRAUD ALERT for user {}: {}", correlationId, userId, e.getMessage());
                    metrics.recordTransactionFraud();
                    metrics.recordFraudRuleHit(e.getMessage());
                    return new TransactionResult(TransactionStatus.FRAUD_DETECTED, e.getMessage(), 0.0);
                }

                metrics.recordTransactionDeclined();
                return new TransactionResult(TransactionStatus.DECLINED, e.getMessage(), 0.0);

            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException ex) {
                        logger.error("[{}] Connection close failed", correlationId, ex);
                    }
                }
            }
        }

        // Should not reach here, but just in case
        metrics.recordTransactionDeclined();
        return new TransactionResult(TransactionStatus.DECLINED, "Max retries exceeded", 0.0);
    }

    private void checkFraud(TransactionContext context, String correlationId) throws FraudDetectedException {
        for (IFraudRule rule : fraudRules) {
            if (rule.isFraudulent(context)) {
                logger.warn("[{}] Fraud rule triggered: {}", correlationId, rule.getRuleName());
                throw new FraudDetectedException(rule.getRuleName());
            }
        }
    }

    private void handleException(Exception e, double amount) {
        if (e instanceof FraudDetectedException) {
            listeners.forEach(l -> l.onFraudDetected(new FraudDetectedEvent(amount, e.getMessage())));
        } else {
            listeners.forEach(l -> l.onDeclined(new TransactionDeclinedEvent(amount, e.getMessage())));
        }
    }

    /**
     * Check if user is rate limited.
     */
    private boolean isRateLimited(String userId) {
        RateLimitTracker tracker = rateLimitMap.computeIfAbsent(userId, k -> new RateLimitTracker());
        return !tracker.allowRequest();
    }

    /**
     * Check if user is in authentication cooldown.
     */
    private boolean isInAuthCooldown(String userId) {
        FailedAuthTracker tracker = failedAuthMap.get(userId);
        if (tracker == null)
            return false;

        if (tracker.isInCooldown()) {
            return true;
        } else {
            // Cooldown expired, remove tracker
            failedAuthMap.remove(userId);
            return false;
        }
    }

    /**
     * Record a failed authentication attempt.
     */
    private void recordFailedAuth(String userId) {
        FailedAuthTracker tracker = failedAuthMap.computeIfAbsent(
                userId, k -> new FailedAuthTracker());
        tracker.recordFailedAttempt();

        if (tracker.failedCount >= MAX_FAILED_AUTH_ATTEMPTS) {
            logger.warn("User {} exceeded max failed auth attempts, entering cooldown", userId);
        }
    }

    /**
     * Check if exception is retryable (deadlock, lock timeout).
     */
    private boolean isRetryableException(SQLException e) {
        String sqlState = e.getSQLState();
        // PostgreSQL deadlock: 40P01, Lock timeout: 55P03
        return "40P01".equals(sqlState) || "55P03".equals(sqlState) ||
                e.getMessage().contains("deadlock") || e.getMessage().contains("timeout");
    }

    /**
     * Get metrics instance for monitoring.
     */
    public TransactionMetrics getMetrics() {
        return metrics;
    }

    /**
     * Simple rate limiter using sliding window.
     */
    private class RateLimitTracker {
        private final Queue<Long> requestTimestamps = new LinkedList<>();

        synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();

            // Remove old timestamps outside the window
            while (!requestTimestamps.isEmpty() &&
                    requestTimestamps.peek() < now - RATE_LIMIT_WINDOW_MS) {
                requestTimestamps.poll();
            }

            if (requestTimestamps.size() >= MAX_REQUESTS_PER_MINUTE) {
                return false; // Rate limit exceeded
            }

            requestTimestamps.offer(now);
            return true;
        }
    }

    /**
     * Track failed authentication attempts.
     */
    private class FailedAuthTracker {
        private int failedCount = 0;
        private long firstFailedTime = 0;
        private long cooldownStartTime = 0;

        synchronized void recordFailedAttempt() {
            long now = System.currentTimeMillis();

            if (firstFailedTime == 0 || now - firstFailedTime > RATE_LIMIT_WINDOW_MS) {
                // Reset counter if outside window
                failedCount = 1;
                firstFailedTime = now;
            } else {
                failedCount++;
                if (failedCount >= MAX_FAILED_AUTH_ATTEMPTS) {
                    cooldownStartTime = now;
                }
            }
        }

        synchronized boolean isInCooldown() {
            if (cooldownStartTime == 0)
                return false;

            long now = System.currentTimeMillis();
            return now - cooldownStartTime < AUTH_COOLDOWN_MS;
        }
    }
}
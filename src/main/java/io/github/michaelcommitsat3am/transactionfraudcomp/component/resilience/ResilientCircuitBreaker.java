package io.github.michaelcommitsat3am.transactionfraudcomp.component.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Circuit breaker implementation for protecting against cascading failures
 * when external dependencies (like Redis) become unavailable.
 */
public class ResilientCircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(ResilientCircuitBreaker.class);

    private final CircuitBreaker circuitBreaker;

    /**
     * Creates a circuit breaker with default configuration.
     * - Failure rate threshold: 50%
     * - Wait duration in open state: 60 seconds
     * - Sliding window size: 10 calls
     */
    public ResilientCircuitBreaker(String name) {
        this(name, 50.0f, Duration.ofSeconds(60), 10);
    }

    /**
     * Creates a circuit breaker with custom configuration.
     * 
     * @param name                    Circuit breaker name
     * @param failureRateThreshold    Percentage of failures to trigger open state
     *                                (0-100)
     * @param waitDurationInOpenState Time to wait before attempting recovery
     * @param slidingWindowSize       Number of calls to track for metrics
     */
    public ResilientCircuitBreaker(
            String name,
            float failureRateThreshold,
            Duration waitDurationInOpenState,
            int slidingWindowSize) {

        logger.info("Creating circuit breaker: {}", name);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(waitDurationInOpenState)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.circuitBreaker = registry.circuitBreaker(name);

        // Register event listeners
        circuitBreaker.getEventPublisher()
                .onStateTransition(event -> logger.warn("Circuit breaker '{}' state transition: {} -> {}",
                        name, event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState()))
                .onError(event -> logger.debug("Circuit breaker '{}' recorded error: {}",
                        name, event.getThrowable().getMessage()))
                .onSuccess(event -> logger.trace("Circuit breaker '{}' recorded success", name));

        logger.info("✅ Circuit breaker '{}' initialized - Threshold: {}%, Window: {}",
                name, failureRateThreshold, slidingWindowSize);
    }

    /**
     * Executes a supplier with circuit breaker protection.
     * 
     * @param supplier The operation to execute
     * @return The result of the supplier
     * @throws Exception if the operation fails or circuit is open
     */
    public <T> T executeSupplier(Supplier<T> supplier) throws Exception {
        Supplier<T> decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        return decoratedSupplier.get();
    }

    /**
     * Executes a runnable with circuit breaker protection.
     * 
     * @param runnable The operation to execute
     * @throws Exception if the operation fails or circuit is open
     */
    public void executeRunnable(Runnable runnable) throws Exception {
        Runnable decoratedRunnable = CircuitBreaker.decorateRunnable(circuitBreaker, runnable);
        decoratedRunnable.run();
    }

    /**
     * Gets the current state of the circuit breaker.
     */
    public CircuitBreaker.State getState() {
        return circuitBreaker.getState();
    }

    /**
     * Gets metrics from the circuit breaker.
     */
    public CircuitBreaker.Metrics getMetrics() {
        return circuitBreaker.getMetrics();
    }

    /**
     * Resets the circuit breaker to closed state.
     */
    public void reset() {
        logger.info("Resetting circuit breaker");
        circuitBreaker.reset();
    }

    /**
     * Transitions circuit breaker to open state (for testing).
     */
    public void transitionToOpenState() {
        logger.warn("Manually opening circuit breaker");
        circuitBreaker.transitionToOpenState();
    }
}

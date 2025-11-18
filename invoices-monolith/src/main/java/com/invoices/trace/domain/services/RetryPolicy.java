package com.invoices.trace.domain.services;

import java.util.function.Supplier;

/**
 * Domain service for retry logic.
 * Implements exponential backoff strategy for retrying operations.
 */
public class RetryPolicy {

    private final int maxRetries;
    private final long initialDelayMs;

    /**
     * Creates a retry policy with the specified parameters.
     *
     * @param maxRetries     maximum number of retry attempts
     * @param initialDelayMs initial delay in milliseconds before first retry
     */
    public RetryPolicy(int maxRetries, long initialDelayMs) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("Max retries must be non-negative");
        }
        if (initialDelayMs < 0) {
            throw new IllegalArgumentException("Initial delay must be non-negative");
        }

        this.maxRetries = maxRetries;
        this.initialDelayMs = initialDelayMs;
    }

    /**
     * Default retry policy: 3 retries with 1 second initial delay.
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(3, 1000);
    }

    /**
     * Executes an operation with retry logic using exponential backoff.
     *
     * @param operation the operation to execute
     * @param <T>       the return type of the operation
     * @return the result of the operation
     * @throws Exception if all retry attempts fail
     */
    public <T> T execute(Supplier<T> operation) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                return operation.get();

            } catch (Exception e) {
                lastException = e;

                if (attempt > maxRetries) {
                    // All retries exhausted
                    throw new RuntimeException(
                            "Operation failed after " + maxRetries + " retries",
                            lastException
                    );
                }

                // Calculate exponential backoff delay
                long backoffDelay = calculateBackoffDelay(attempt);

                try {
                    Thread.sleep(backoffDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        // This should never be reached, but compiler requires it
        throw new RuntimeException("Unexpected retry state", lastException);
    }

    /**
     * Executes a void operation (Runnable) with retry logic.
     *
     * @param operation the operation to execute
     * @throws Exception if all retry attempts fail
     */
    public void executeVoid(Runnable operation) throws Exception {
        execute(() -> {
            operation.run();
            return null;
        });
    }

    /**
     * Calculates the backoff delay for a given attempt number using exponential backoff.
     * Formula: initialDelay * 2^(attempt - 1)
     *
     * @param attempt the current attempt number (1-indexed)
     * @return the delay in milliseconds
     */
    private long calculateBackoffDelay(int attempt) {
        return initialDelayMs * (long) Math.pow(2, attempt - 1);
    }

    /**
     * Gets the maximum number of retries.
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Gets the initial delay in milliseconds.
     */
    public long getInitialDelayMs() {
        return initialDelayMs;
    }
}

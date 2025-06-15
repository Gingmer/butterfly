package com.butterfly.framework.core.rpc.circuitbreaker;

import com.butterfly.framework.core.config.CircuitBreakerProperties;
import com.butterfly.framework.core.faulttolerance.CircuitBreaker;
import com.butterfly.framework.core.faulttolerance.CircuitBreakerMethod;
import com.butterfly.framework.core.faulttolerance.CircuitBreakerOpenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认熔断器实现
 * 基于失败率和状态机实现熔断逻辑
 */
public class DefaultCircuitBreaker implements CircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCircuitBreaker.class);

    private final String serviceName;
    private final int failureThreshold;
    private final int recoveryAttempts;
    private final long resetTimeout;

    private volatile CircuitState state = CircuitState.CLOSED;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);

    public DefaultCircuitBreaker(String serviceName, CircuitBreakerProperties properties) {
        this.serviceName = serviceName;
        this.failureThreshold = properties.getFailureThreshold();
        this.recoveryAttempts = properties.getHalfOpenMaxAttempts();
        this.resetTimeout = properties.getResetTimeoutMillis();
    }

    @Override
    @Override
public <T> T execute(CircuitBreakerMethod<T> method) throws Exception {
        checkStateTransition();

        if (state == CircuitState.OPEN) {
            throw new CircuitBreakerOpenException("Circuit breaker is open for service: " + serviceName);
        }

        try {
            T result = method.invoke();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private void checkStateTransition() {
        if (state == CircuitState.OPEN) {
            long now = System.currentTimeMillis();
            if (now - lastFailureTime.get() > resetTimeout) {
                state = CircuitState.HALF_OPEN;
                logger.info("Circuit breaker transitioning to HALF_OPEN for service: {}", serviceName);
            }
        }
    }

    private void onSuccess() {
        switch (state) {
            case CLOSED:
                failureCount.set(0);
                break;
            case HALF_OPEN:
                int currentSuccess = successCount.incrementAndGet();
                if (currentSuccess >= recoveryAttempts) {
                    state = CircuitState.CLOSED;
                    successCount.set(0);
                    failureCount.set(0);
                    logger.info("Circuit breaker transitioning to CLOSED for service: {}", serviceName);
                }
                break;
        }
    }

    private void onFailure() {
        switch (state) {
            case CLOSED:
                int currentFailure = failureCount.incrementAndGet();
                if (currentFailure >= failureThreshold) {
                    state = CircuitState.OPEN;
                    lastFailureTime.set(System.currentTimeMillis());
                    logger.info("Circuit breaker transitioning to OPEN for service: {}", serviceName);
                }
                break;
            case HALF_OPEN:
                state = CircuitState.OPEN;
                lastFailureTime.set(System.currentTimeMillis());
                successCount.set(0);
                logger.info("Circuit breaker transitioning back to OPEN for service: {}", serviceName);
                break;
        }
    }

    /**
     * 熔断器状态枚举
     */
    public enum CircuitState {
        CLOSED,    // 闭合状态：正常请求
        OPEN,      // 打开状态：拒绝请求
        HALF_OPEN  // 半开状态：尝试恢复
    }

    /**
     * 熔断器打开异常
     */
    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
package com.butterfly.framework.core.faulttolerance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 默认熔断器实现
 * 基于失败计数和时间窗口的熔断机制
 */
public class DefaultCircuitBreaker implements CircuitBreaker {
    private static final Logger logger = LoggerFactory.getLogger(DefaultCircuitBreaker.class);

    // 熔断器名称，用于日志和监控
    private final String name;
    // 失败阈值：达到此失败次数则触发熔断
    private final int failureThreshold;
    // 重置超时时间：熔断器打开后，经过此时间进入半开状态
    private final long resetTimeoutMillis;
    // 请求 volume 阈值：在时间窗口内至少需要这么多请求才考虑熔断
    private final int requestVolumeThreshold;
    // 半开状态下允许的试探请求数
    private final int halfOpenMaxAttempts;

    // 当前熔断器状态
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    // 失败计数器
    private final AtomicInteger failureCount = new AtomicInteger(0);
    // 请求计数器
    private final AtomicInteger requestCount = new AtomicInteger(0);
    // 半开状态下的成功计数器
    private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
    // 熔断器打开时间戳
    private final AtomicLong openTimestamp = new AtomicLong(0);
    // 状态更新锁
    private final ReentrantLock stateLock = new ReentrantLock();

    /**
     * 构造函数
     * @param name 熔断器名称
     * @param failureThreshold 失败阈值
     * @param resetTimeoutMillis 重置超时时间(毫秒)
     * @param requestVolumeThreshold 请求volume阈值
     * @param halfOpenMaxAttempts 半开状态最大尝试次数
     */
    public DefaultCircuitBreaker(String name, int failureThreshold, long resetTimeoutMillis,
                                int requestVolumeThreshold, int halfOpenMaxAttempts) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMillis = resetTimeoutMillis;
        this.requestVolumeThreshold = requestVolumeThreshold;
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
    }

    @Override
    public <T> T execute(CircuitBreakerMethod<T> method) throws Exception {
        // 检查并更新状态
        checkAndUpdateState();

        if (state == CircuitBreakerState.OPEN) {
            // 打开状态，直接抛出熔断异常
            throw new CircuitBreakerOpenException("Circuit breaker '" + name + "' is OPEN");
        }

        try {
            // 执行目标方法
            T result = method.invoke();

            // 执行成功，处理成功逻辑
            onSuccess();
            return result;
        } catch (Exception e) {
            // 执行失败，处理失败逻辑
            onFailure();
            throw e;
        }
    }

    /**
     * 检查并更新熔断器状态
     */
    private void checkAndUpdateState() {
        switch (state) {
            case CLOSED:
                // 闭合状态：检查是否达到失败阈值
                if (requestCount.get() >= requestVolumeThreshold &&
                        failureCount.get() >= failureThreshold) {
                    // 达到阈值，切换到打开状态
                    transitionToOpen();
                }
                break;
            case OPEN:
                // 打开状态：检查是否达到重置时间
                long currentTime = System.currentTimeMillis();
                if (currentTime - openTimestamp.get() >= resetTimeoutMillis) {
                    // 达到重置时间，切换到半开状态
                    transitionToHalfOpen();
                }
                break;
            case HALF_OPEN:
                // 半开状态：已在execute方法中处理
                break;
        }
    }

    /**
     * 切换到打开状态
     */
    private void transitionToOpen() {
        stateLock.lock();
        try {
            if (state != CircuitBreakerState.OPEN) {
                state = CircuitBreakerState.OPEN;
                openTimestamp.set(System.currentTimeMillis());
                logger.warn("Circuit breaker '{}' transitioned to OPEN state. Failure count: {}/{}",
                        name, failureCount.get(), failureThreshold);
                // 重置计数器
                resetCounts();
            }
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 切换到半开状态
     */
    private void transitionToHalfOpen() {
        stateLock.lock();
        try {
            if (state == CircuitBreakerState.OPEN) {
                state = CircuitBreakerState.HALF_OPEN;
                logger.info("Circuit breaker '{}' transitioned to HALF_OPEN state after {}ms timeout",
                        name, resetTimeoutMillis);
                // 重置半开状态计数器
                halfOpenSuccessCount.set(0);
            }
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 切换到闭合状态
     */
    private void transitionToClosed() {
        stateLock.lock();
        try {
            if (state != CircuitBreakerState.CLOSED) {
                state = CircuitBreakerState.CLOSED;
                logger.info("Circuit breaker '{}' transitioned to CLOSED state. Recovery successful.", name);
                // 重置计数器
                resetCounts();
            }
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 处理成功逻辑
     */
    private void onSuccess() {
        switch (state) {
            case CLOSED:
                // 闭合状态：重置失败计数
                failureCount.set(0);
                break;
            case HALF_OPEN:
                // 半开状态：增加成功计数，达到阈值则切换到闭合状态
                int successCount = halfOpenSuccessCount.incrementAndGet();
                if (successCount >= halfOpenMaxAttempts) {
                    transitionToClosed();
                }
                break;
            case OPEN:
                // 打开状态：不应到达此分支
                break;
        }
        requestCount.incrementAndGet();
    }

    /**
     * 处理失败逻辑
     */
    private void onFailure() {
        switch (state) {
            case CLOSED:
                // 闭合状态：增加失败计数
                failureCount.incrementAndGet();
                break;
            case HALF_OPEN:
                // 半开状态：任何失败都切换回打开状态
                transitionToOpen();
                break;
            case OPEN:
                // 打开状态：不应到达此分支
                break;
        }
        requestCount.incrementAndGet();
    }

    /**
     * 重置计数器
     */
    private void resetCounts() {
        failureCount.set(0);
        requestCount.set(0);
        halfOpenSuccessCount.set(0);
    }

    @Override
    public CircuitBreakerState getState() {
        checkAndUpdateState();
        return state;
    }

    @Override
    public void reset() {
        stateLock.lock();
        try {
            state = CircuitBreakerState.CLOSED;
            resetCounts();
            logger.info("Circuit breaker '{}' has been manually reset to CLOSED state", name);
        } finally {
            stateLock.unlock();
        }
    }

    /**
     * 熔断器打开异常
     */
    public static class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }
}
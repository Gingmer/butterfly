package com.butterfly.framework.core.faulttolerance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.butterfly.framework.core.config.RetryPolicyProperties;
import com.butterfly.framework.core.exception.RetryInterruptedException;
import com.butterfly.framework.core.exception.RetryExhaustedException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.butterfly.framework.core.faulttolerance.CircuitBreaker;
import com.butterfly.framework.core.faulttolerance.RetryPolicy;

/**
 * 指数退避重试策略实现
 * 失败后按指数增长的间隔时间进行重试，避免服务过载
 */
public class ExponentialBackoffRetryPolicy<T> implements RetryPolicy {
    private static final Logger logger = LoggerFactory.getLogger(ExponentialBackoffRetryPolicy.class);

    // 最大重试次数
    private final int maxRetries;
    // 初始重试延迟(毫秒)
    private final long initialDelayMillis;
    // 最大重试延迟(毫秒)
    private final long maxDelayMillis;
    // 延迟乘数
    private final double multiplier;
    // 当前重试计数
    private final AtomicInteger currentRetryCount = new AtomicInteger(0);

    /**
 * 构造函数
 * @param properties 重试策略配置属性
 */
public ExponentialBackoffRetryPolicy(RetryPolicyProperties properties) {
    Objects.requireNonNull(properties, "RetryPolicyProperties cannot be null");
    if (properties.getMaxRetries() < 0) {
        throw new IllegalArgumentException("maxRetries must be non-negative");
    }
    if (properties.getInitialDelayMillis() <= 0) {
        throw new IllegalArgumentException("initialDelay must be positive");
    }
    if (properties.getMaxDelayMillis() < properties.getInitialDelayMillis()) {
        throw new IllegalArgumentException("maxDelay must be greater than or equal to initialDelay");
    }
    if (properties.getMultiplier() <= 1.0) {
        throw new IllegalArgumentException("multiplier must be greater than 1.0");
    }

    this.maxRetries = properties.getMaxRetries();
    this.initialDelayMillis = properties.getInitialDelayMillis();
    this.maxDelayMillis = properties.getMaxDelayMillis();
    this.multiplier = properties.getMultiplier();
}

    @Override
    public <T> T execute(CircuitBreaker.CircuitBreakerMethod<T> method) throws Exception {
        currentRetryCount.set(0); // 重置重试计数器
        Throwable lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    long delay = calculateDelay(attempt);
                logger.info("重试第{}次，等待{}ms...", attempt, delay);
                currentRetryCount.set(attempt); // 更新重试计数器
                    TimeUnit.MILLISECONDS.sleep(delay);
                }

                return method.invoke();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RetryInterruptedException("重试被中断", e);
            } catch (Exception e) {
                lastException = e;
                logger.warn("第{}次调用失败: {}", attempt, e.getMessage());
            }
        }

        // 重试耗尽，抛出最后一次异常
        throw new RetryExhaustedException("达到最大重试次数" + maxRetries, lastException);
    }

    /**
     * 计算重试延迟时间
     * @param attempt 当前重试次数(从1开始)
     * @return 延迟时间(毫秒)
     */
    private long calculateDelay(int attempt) {
        if (attempt <= 0) {
            return initialDelayMillis;
        }
        // 使用循环计算指数值，避免Math.pow()的性能开销
        long delay = initialDelayMillis;
        for (int i = 1; i < attempt; i++) {
            delay = (long) (delay * multiplier);
            if (delay >= maxDelayMillis) {
                break;
            }
        }
        return Math.min(delay, maxDelayMillis);
    }



    @Override
    public int getCurrentRetryCount() {
        return currentRetryCount.get();
    }

    @Override
    public void reset() {
        currentRetryCount.set(0);
    }
}
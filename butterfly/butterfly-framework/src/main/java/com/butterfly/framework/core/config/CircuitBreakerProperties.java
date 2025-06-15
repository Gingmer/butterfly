package com.butterfly.framework.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 熔断器配置属性类
 * 用于外部化配置熔断器相关参数
 */
@Component
@ConfigurationProperties(prefix = "rpc.circuit-breaker")
public class CircuitBreakerProperties {
    /**
     * 是否启用熔断器
     */
    private boolean enabled = true;

    /**
     * 失败阈值：达到此失败次数则触发熔断
     */
    private int failureThreshold = 5;

    /**
     * 重置超时时间：熔断器打开后，经过此时间进入半开状态(毫秒)
     */
    private long resetTimeoutMillis = 60000;

    /**
     * 请求volume阈值：在时间窗口内至少需要这么多请求才考虑熔断
     */
    private int requestVolumeThreshold = 20;

    /**
     * 半开状态下允许的试探请求数
     */
    private int halfOpenMaxAttempts = 5;

    /**
     * 统计时间窗口大小(毫秒)
     */
    private long statisticalWindowMillis = 60000;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public long getResetTimeoutMillis() {
        return resetTimeoutMillis;
    }

    public void setResetTimeoutMillis(long resetTimeoutMillis) {
        this.resetTimeoutMillis = resetTimeoutMillis;
    }

    public int getRequestVolumeThreshold() {
        return requestVolumeThreshold;
    }

    public void setRequestVolumeThreshold(int requestVolumeThreshold) {
        this.requestVolumeThreshold = requestVolumeThreshold;
    }

    public int getHalfOpenMaxAttempts() {
        return halfOpenMaxAttempts;
    }

    public void setHalfOpenMaxAttempts(int halfOpenMaxAttempts) {
        this.halfOpenMaxAttempts = halfOpenMaxAttempts;
    }

    public long getStatisticalWindowMillis() {
        return statisticalWindowMillis;
    }

    public void setStatisticalWindowMillis(long statisticalWindowMillis) {
        this.statisticalWindowMillis = statisticalWindowMillis;
    }
}
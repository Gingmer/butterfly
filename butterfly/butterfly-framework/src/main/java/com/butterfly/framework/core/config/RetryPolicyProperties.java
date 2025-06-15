package com.butterfly.framework.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 重试策略配置属性类
 * 用于外部化配置重试机制相关参数
 */
@Component
@ConfigurationProperties(prefix = "rpc.retry")
public class RetryPolicyProperties {
    /**
     * 是否启用重试机制
     */
    private boolean enabled = true;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    /**
     * 初始重试延迟(毫秒)
     */
    private long initialDelayMillis = 100;

    /**
     * 最大重试延迟(毫秒)
     */
    private long maxDelayMillis = 1000;

    /**
     * 延迟乘数(指数退避)
     */
    private double multiplier = 2.0;

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getInitialDelayMillis() {
        return initialDelayMillis;
    }

    public void setInitialDelayMillis(long initialDelayMillis) {
        this.initialDelayMillis = initialDelayMillis;
    }

    public long getMaxDelayMillis() {
        return maxDelayMillis;
    }

    public void setMaxDelayMillis(long maxDelayMillis) {
        this.maxDelayMillis = maxDelayMillis;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

}
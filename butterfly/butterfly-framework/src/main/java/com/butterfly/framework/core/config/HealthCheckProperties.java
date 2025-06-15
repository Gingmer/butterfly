package com.butterfly.framework.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 健康检查配置属性类
 * 用于外部化配置健康检查相关参数
 */
@Component
@ConfigurationProperties(prefix = "rpc.health-check")
public class HealthCheckProperties {
    /**
     * 健康检查间隔时间(毫秒)
     */
    private int intervalMillis = 10000;

    /**
     * 健康检查超时时间(毫秒)
     */
    private int timeoutMillis = 3000;

    /**
     * 最大连续失败次数阈值
     */
    private int maxFailureCount = 3;

    /**
     * 是否启用健康检查
     */
    private boolean enabled = true;

    // Getters and Setters
    public int getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(int intervalMillis) {
        this.intervalMillis = intervalMillis;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public int getMaxFailureCount() {
        return maxFailureCount;
    }

    public void setMaxFailureCount(int maxFailureCount) {
        this.maxFailureCount = maxFailureCount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
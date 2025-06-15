package com.butterfly.framework.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 负载均衡配置属性类
 * 用于外部化配置负载均衡相关参数
 */
@Component
@ConfigurationProperties(prefix = "rpc.load-balance")
public class LoadBalanceProperties {
    /**
     * 默认负载均衡策略
     */
    private String defaultStrategy = "roundRobin";

    /**
     * 负载均衡策略参数
     */
    private StrategyProperties strategy = new StrategyProperties();

    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    public void setDefaultStrategy(String defaultStrategy) {
        this.defaultStrategy = defaultStrategy;
    }

    public StrategyProperties getStrategy() {
        return strategy;
    }

    public void setStrategy(StrategyProperties strategy) {
        this.strategy = strategy;
    }

    /**
     * 负载均衡策略的具体参数
     */
    public static class StrategyProperties {
        /**
         * 权重随机策略的权重配置
         */
        private String weightedRandomWeights;

        /**
         * 最少活跃策略的阈值
         */
        private int leastActiveThreshold = 10;

        public String getWeightedRandomWeights() {
            return weightedRandomWeights;
        }

        public void setWeightedRandomWeights(String weightedRandomWeights) {
            this.weightedRandomWeights = weightedRandomWeights;
        }

        public int getLeastActiveThreshold() {
            return leastActiveThreshold;
        }

        public void setLeastActiveThreshold(int leastActiveThreshold) {
            this.leastActiveThreshold = leastActiveThreshold;
        }
    }
}
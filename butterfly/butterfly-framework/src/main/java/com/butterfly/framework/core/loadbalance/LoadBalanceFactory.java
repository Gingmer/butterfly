package com.butterfly.framework.core.loadbalance;

import com.butterfly.framework.annotation.LoadBalanceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.butterfly.framework.core.config.LoadBalanceProperties;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 企业级负载均衡工厂
 * 管理负载均衡策略的创建、缓存和获取，支持SPI扩展
 */
@Component
public class LoadBalanceFactory {
    private static final Logger logger = LoggerFactory.getLogger(LoadBalanceFactory.class);
    private final String defaultLoadBalanceStrategy;

    // 负载均衡策略缓存
    private final Map<String, LoadBalance> loadBalanceCache = new ConcurrentHashMap<>();
    // 负载均衡策略映射
    private final LoadBalanceProperties properties;
    private final Map<String, Class<? extends LoadBalance>> loadBalanceMap;

    @Autowired
    public LoadBalanceFactory(LoadBalanceProperties properties) {
        // 初始化内置负载均衡策略
        this.properties = properties;
        this.defaultLoadBalanceStrategy = properties.getDefaultStrategy();
        loadBalanceMap = new HashMap<>();
        loadBalanceMap.put(defaultLoadBalanceStrategy, RoundRobinLoadBalance.class);
        

        // 通过SPI加载自定义负载均衡策略
        loadSpiLoadBalances();

        logger.info("负载均衡工厂初始化完成，支持的策略: {}", loadBalanceMap.keySet());
    }

    /**
     * 获取负载均衡实例
     * @param strategy 负载均衡策略名称
     * @return 负载均衡实例
     */
    public LoadBalance getLoadBalance(String strategy) {
        if (strategy == null || strategy.trim().isEmpty()) {
            strategy = defaultLoadBalanceStrategy;
        }

        // 从缓存获取
        LoadBalance loadBalance = loadBalanceCache.get(strategy);
        if (loadBalance != null) {
            return loadBalance;
        }

        // 创建新实例
        synchronized (this) {
            loadBalance = loadBalanceCache.get(strategy);
            if (loadBalance == null) {
                Class<? extends LoadBalance> loadBalanceClass = loadBalanceMap.get(strategy);
                if (loadBalanceClass == null) {
                    logger.warn("未知的负载均衡策略: {}, 使用默认策略: {}", strategy, defaultLoadBalanceStrategy);
            loadBalanceClass = loadBalanceMap.get(defaultLoadBalanceStrategy);
                }

                try {
                    loadBalance = loadBalanceClass.newInstance();
                    loadBalanceCache.put(strategy, loadBalance);
                    logger.info("创建负载均衡实例: {}", strategy);
                } catch (InstantiationException | IllegalAccessException e) {
                    logger.error("创建负载均衡实例失败: {}", strategy, e);
                    throw new RuntimeException("Failed to create load balance instance: " + strategy, e);
                }
            }
        }

        return loadBalance;
    }

    /**
     * 通过SPI加载自定义负载均衡策略
     */
    private void loadSpiLoadBalances() {
        try {
            ServiceLoader<LoadBalance> serviceLoader = ServiceLoader.load(LoadBalance.class);
            for (LoadBalance loadBalance : serviceLoader) {
                LoadBalanceStrategy annotation = loadBalance.getClass().getAnnotation(LoadBalanceStrategy.class);
                if (annotation != null && !annotation.value().isEmpty()) {
                    String strategyName = annotation.value();
                    loadBalanceMap.put(strategyName, loadBalance.getClass());
                    loadBalanceCache.put(strategyName, loadBalance);
                    logger.info("通过SPI加载自定义负载均衡策略: {}", strategyName);
                }
            }
        } catch (Exception e) {
            logger.warn("加载SPI负载均衡策略失败", e);
        }
    }

    /**
     * 注册自定义负载均衡策略
     * @param strategyName 策略名称
     * @param loadBalanceClass 策略类
     */
    public void registerLoadBalance(String strategyName, Class<? extends LoadBalance> loadBalanceClass) {
        if (loadBalanceMap.containsKey(strategyName)) {
            logger.warn("负载均衡策略已存在: {}", strategyName);
            return;
        }

        loadBalanceMap.put(strategyName, loadBalanceClass);
        // 清除缓存
        loadBalanceCache.remove(strategyName);
        logger.info("注册自定义负载均衡策略: {}", strategyName);
    }
}
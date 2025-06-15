package com.butterfly.framework.core.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;
import com.butterfly.framework.core.config.HealthCheckProperties;

/**
 * 企业级内存服务注册中心实现
 * 适用于开发环境和单机测试，提供基础的服务注册与发现能力
 */
@Component
public class InMemoryServiceRegistry implements ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryServiceRegistry.class);

    // 服务注册表: serviceName -> 地址列表
    private final Map<String, List<InetSocketAddress>> serviceRegistry = new ConcurrentHashMap<>();
    // 服务元数据: serviceName -> address -> metadata
    private final Map<String, Map<InetSocketAddress, Map<String, String>>> serviceMetadata = new ConcurrentHashMap<>();
    // 服务变更监听器: serviceName -> 监听器列表
    private final Map<String, List<ServiceChangeListener>> listeners = new ConcurrentHashMap<>();
    private final HealthChecker healthChecker;
    private final HealthCheckProperties healthCheckProperties;
    private final ScheduledExecutorService healthCheckExecutor;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    // 服务健康状态记录: serviceName -> address -> 连续失败次数
    private final Map<String, Map<InetSocketAddress, Integer>> serviceFailureCounts = new ConcurrentHashMap<>();

    @Autowired
    public InMemoryServiceRegistry(HealthChecker healthChecker, HealthCheckProperties healthCheckProperties) {
        this.healthChecker = healthChecker;
        this.healthCheckProperties = healthCheckProperties;
        this.healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "service-health-checker");
            thread.setDaemon(true);
            return thread;
        });
        if (healthCheckProperties.isEnabled()) {
            startHealthCheck();
        } else {
            logger.info("健康检查已禁用");
        }
    }

    /**
     * 启动健康检查调度任务
     */
    private void startHealthCheck() {
        if (isRunning.compareAndSet(false, true)) {
            int interval = healthChecker != null ? healthChecker.getIntervalMillis() : healthCheckProperties.getIntervalMillis();
            healthCheckExecutor.scheduleAtFixedRate(this::performHealthChecks, 0, interval, TimeUnit.MILLISECONDS);
            logger.info("服务健康检查已启动，间隔: {}ms", interval);
        }
    }

    /**
     * 执行所有服务的健康检查
     */
    private void performHealthChecks() {
        if (healthChecker == null) {
            logger.debug("未配置健康检查器，跳过健康检查");
            return;
        }

        try {
            // 遍历所有服务
            for (String serviceName : serviceRegistry.keySet()) {
                List<InetSocketAddress> addresses = new ArrayList<>(serviceRegistry.get(serviceName));
                for (InetSocketAddress address : addresses) {
                    checkServiceHealth(serviceName, address);
                }
            }
        } catch (Exception e) {
            logger.error("执行健康检查异常", e);
        }
    }

    /**
     * 检查单个服务实例的健康状态
     */
    private void checkServiceHealth(String serviceName, InetSocketAddress address) {
        try {
            boolean isHealthy = healthChecker.check(serviceName, address);
            if (isHealthy) {
                // 健康，重置失败计数
                resetFailureCount(serviceName, address);
            } else {
                // 不健康，增加失败计数
                int failureCount = incrementFailureCount(serviceName, address);
                logger.warn("服务实例不健康: {} -> {}，连续失败次数: {}/{}",
                        serviceName, address, failureCount, healthCheckProperties.getMaxFailureCount());

                // 如果达到最大失败次数，自动注销服务
                if (failureCount >= healthCheckProperties.getMaxFailureCount()) {
                    unregister(serviceName, address);
                    logger.error("服务实例连续失败次数达到阈值，已自动注销: {} -> {}",
                            serviceName, address);
                }
            }
        } catch (Exception e) {
            logger.error("检查服务健康状态异常: {} -> {}", serviceName, address, e);
            incrementFailureCount(serviceName, address);
        }
    }

    /**
     * 增加失败计数
     */
    private int incrementFailureCount(String serviceName, InetSocketAddress address) {
        Map<InetSocketAddress, Integer> addressFailureCounts = serviceFailureCounts.computeIfAbsent(
                serviceName, k -> new ConcurrentHashMap<>());
        return addressFailureCounts.merge(address, 1, Integer::sum);
    }

    /**
     * 重置失败计数
     */
    private void resetFailureCount(String serviceName, InetSocketAddress address) {
        Map<InetSocketAddress, Integer> addressFailureCounts = serviceFailureCounts.get(serviceName);
        if (addressFailureCounts != null) {
            addressFailureCounts.remove(address);
            if (addressFailureCounts.isEmpty()) {
                serviceFailureCounts.remove(serviceName);
            }
        }
    }

    @Override
    public void register(String serviceName, InetSocketAddress serviceAddress, Map<String, String> metadata) {
        // 添加服务地址
        serviceRegistry.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
                       .add(serviceAddress);

        // 存储服务元数据
        serviceMetadata.computeIfAbsent(serviceName, k -> new ConcurrentHashMap<>())
                       .put(serviceAddress, metadata != null ? new HashMap<>(metadata) : new HashMap<>());

        logger.info("服务注册成功: {} -> {}", serviceName, serviceAddress);

        // 通知服务变更
        notifyServiceChange(serviceName);
    }

    @Override
    public void unregister(String serviceName, InetSocketAddress serviceAddress) {
        // 移除服务地址
        List<InetSocketAddress> addresses = serviceRegistry.get(serviceName);
        if (addresses != null) {
            addresses.remove(serviceAddress);
            if (addresses.isEmpty()) {
                serviceRegistry.remove(serviceName);
                serviceMetadata.remove(serviceName);
            } else {
                // 移除元数据
                Map<InetSocketAddress, Map<String, String>> metadataMap = serviceMetadata.get(serviceName);
                if (metadataMap != null) {
                    metadataMap.remove(serviceAddress);
                }
            }
            logger.info("服务注销成功: {} -> {}", serviceName, serviceAddress);

            // 通知服务变更
            notifyServiceChange(serviceName);
        } else {
            logger.warn("服务不存在，无法注销: {} -> {}", serviceName, serviceAddress);
        }
    }

    @Override
    public List<InetSocketAddress> discover(String serviceName) {
        List<InetSocketAddress> addresses = serviceRegistry.get(serviceName);
        if (addresses == null || addresses.isEmpty()) {
            logger.warn("未找到服务: {}", serviceName);
            return Collections.emptyList();
        }
        // 返回地址列表的副本，防止外部修改
        return new ArrayList<>(addresses);
    }

    @Override
    public void subscribe(String serviceName, ServiceChangeListener listener) {
        listeners.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
                 .add(listener);
        logger.info("订阅服务变更: {}", serviceName);
    }

    @Override
    public void unsubscribe(String serviceName, ServiceChangeListener listener) {
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        if (serviceListeners != null) {
            serviceListeners.remove(listener);
            if (serviceListeners.isEmpty()) {
                listeners.remove(serviceName);
            }
            logger.info("取消订阅服务变更: {}", serviceName);
        }
    }

    /**
     * 通知服务变更
     */
    private void notifyServiceChange(String serviceName) {
        List<ServiceChangeListener> serviceListeners = listeners.get(serviceName);
        if (serviceListeners != null && !serviceListeners.isEmpty()) {
            List<InetSocketAddress> addresses = discover(serviceName);
            for (ServiceChangeListener listener : serviceListeners) {
                try {
                    listener.onServiceChanged(serviceName, addresses);
                } catch (Exception e) {
                    logger.error("服务变更通知失败", e);
                }
            }
        }
    }

    /**
     * 获取服务元数据
     * @param serviceName 服务名称
     * @param address 服务地址
     * @return 元数据
     */
    public Map<String, String> getServiceMetadata(String serviceName, InetSocketAddress address) {
        Map<InetSocketAddress, Map<String, String>> metadataMap = serviceMetadata.get(serviceName);
        if (metadataMap != null) {
            return metadataMap.getOrDefault(address, Collections.emptyMap());
        }
        return Collections.emptyMap();
    }

    /**
     * 销毁资源
     */
    @PreDestroy
    public void destroy() {
        isRunning.set(false);
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
        }
        logger.info("服务注册中心已关闭");
    }


}
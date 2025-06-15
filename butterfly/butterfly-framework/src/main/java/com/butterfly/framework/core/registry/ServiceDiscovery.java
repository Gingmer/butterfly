package com.butterfly.framework.core.registry;

import com.butterfly.framework.core.loadbalance.LoadBalance;
import com.butterfly.framework.core.loadbalance.LoadBalanceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.butterfly.framework.core.config.LoadBalanceProperties;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 企业级服务发现组件
 * 集成服务注册中心与负载均衡，提供服务实例的发现与选择能力
 */
@Component
public class ServiceDiscovery implements ServiceRegistry.ServiceChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(ServiceDiscovery.class);
    private final String defaultLoadBalanceStrategy;

    private final ServiceRegistry serviceRegistry;
    private final LoadBalanceFactory loadBalanceFactory;
    private final Map<String, List<InetSocketAddress>> serviceCache;
    private final LoadBalanceProperties loadBalanceProperties;
    private final Map<String, ReentrantLock> serviceLocks;

    @Autowired
    public ServiceDiscovery(ServiceRegistry serviceRegistry, LoadBalanceFactory loadBalanceFactory, LoadBalanceProperties loadBalanceProperties) {
        this.serviceRegistry = serviceRegistry;
        this.loadBalanceFactory = loadBalanceFactory;
        this.serviceCache = new ConcurrentHashMap<>();
        this.loadBalanceProperties = loadBalanceProperties;
        this.defaultLoadBalanceStrategy = loadBalanceProperties.getDefaultStrategy();
        this.serviceLocks = new ConcurrentHashMap<>();
    }

    /**
     * 发现服务并选择一个实例
     * @param serviceName 服务名称
     * @return 选中的服务地址
     */
    public InetSocketAddress discoverService(String serviceName) {
        return discoverService(serviceName, defaultLoadBalanceStrategy);
    }

    /**
     * 发现服务并使用指定负载均衡策略选择实例
     * @param serviceName 服务名称
     * @param loadBalanceStrategy 负载均衡策略
     * @return 选中的服务地址
     */
    public InetSocketAddress discoverService(String serviceName, String loadBalanceStrategy) {
        // 获取服务地址列表
        List<InetSocketAddress> addresses = getServiceAddresses(serviceName);
        if (addresses.isEmpty()) {
            logger.error("未找到可用服务实例: {}", serviceName);
            throw new IllegalStateException("No available service instances: " + serviceName);
        }

        // 获取负载均衡器并选择实例
        LoadBalance loadBalance = loadBalanceFactory.getLoadBalance(loadBalanceStrategy);
        return loadBalance.select(addresses, serviceName);
    }

    /**
     * 获取服务地址列表（带缓存机制）
     */
    private List<InetSocketAddress> getServiceAddresses(String serviceName) {
        // 先从缓存获取
        List<InetSocketAddress> addresses = serviceCache.get(serviceName);

        // 缓存未命中或为空，从注册中心获取
        if (addresses == null || addresses.isEmpty()) {
            ReentrantLock lock = serviceLocks.computeIfAbsent(serviceName, k -> new ReentrantLock());
            lock.lock();
            try {
                // 双重检查
                addresses = serviceCache.get(serviceName);
                if (addresses == null || addresses.isEmpty()) {
                    // 从注册中心获取
                    addresses = serviceRegistry.discover(serviceName);
                    if (addresses != null && !addresses.isEmpty()) {
                        // 缓存服务地址
                        serviceCache.put(serviceName, new CopyOnWriteArrayList<>(addresses));
                        // 订阅服务变更
                        serviceRegistry.subscribe(serviceName, this);
                        logger.info("缓存服务地址: {} -> {}", serviceName, addresses.size());
                    } else {
                        serviceCache.put(serviceName, new CopyOnWriteArrayList<>());
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return addresses != null ? new ArrayList<>(addresses) : new ArrayList<>();
    }

    /**
     * 服务变更通知处理
     */
    @Override
    public void onServiceChanged(String serviceName, List<InetSocketAddress> newServiceAddresses) {
        logger.info("服务地址变更: {} -> 新实例数: {}", serviceName, newServiceAddresses.size());
        // 更新缓存
        serviceCache.put(serviceName, new CopyOnWriteArrayList<>(newServiceAddresses));
    }

    /**
     * 获取服务元数据
     */
    public Map<String, String> getServiceMetadata(String serviceName, InetSocketAddress address) {
        return serviceRegistry.getServiceMetadata(serviceName, address);
    }
}
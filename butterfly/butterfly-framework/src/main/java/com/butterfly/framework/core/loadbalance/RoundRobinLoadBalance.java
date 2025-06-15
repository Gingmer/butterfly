package com.butterfly.framework.core.loadbalance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业级轮询负载均衡实现
 * 按顺序循环选择服务实例，确保请求均匀分配
 */
public class RoundRobinLoadBalance implements LoadBalance {
    private static final Logger logger = LoggerFactory.getLogger(RoundRobinLoadBalance.class);
    // 服务选择计数器，使用原子类保证线程安全
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName) {
        if (addresses == null || addresses.isEmpty()) {
            logger.error("服务地址列表为空，无法选择服务实例: {}", serviceName);
            return null;
        }

        int addressCount = addresses.size();
        // 防止计数器溢出，取模确保在有效范围内
        int index = Math.abs(counter.getAndIncrement()) % addressCount;
        InetSocketAddress selectedAddress = addresses.get(index);

        logger.debug("轮询负载均衡选择服务实例: {}[{}] -> {}",
                serviceName, index, selectedAddress);

        return selectedAddress;
    }
}
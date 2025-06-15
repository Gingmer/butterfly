package com.butterfly.framework.core.registry;

import java.net.InetSocketAddress;

/**
 * 企业级服务健康检查接口
 * 定义服务健康状态检查的标准契约
 */
public interface HealthChecker {
    /**
     * 检查服务健康状态
     * @param serviceName 服务名称
     * @param address 服务地址
     * @return true表示健康，false表示不健康
     */
    boolean check(String serviceName, InetSocketAddress address);

    /**
     * 获取健康检查超时时间(毫秒)
     */
    default int getTimeoutMillis() {
        return 3000;
    }

    /**
     * 获取健康检查间隔(毫秒)
     */
    default int getIntervalMillis() {
        return 10000;
    }
}
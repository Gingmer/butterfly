package com.butterfly.framework.core.loadbalance;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 企业级负载均衡接口
 * 定义服务实例选择的标准契约，支持多种负载均衡策略
 */
public interface LoadBalance {
    /**
     * 从服务地址列表中选择一个实例
     * @param addresses 服务地址列表
     * @param serviceName 服务名称，用于日志和特殊策略
     * @return 选中的服务地址
     */
    InetSocketAddress select(List<InetSocketAddress> addresses, String serviceName);

    /**
     * 通知负载均衡器服务调用结果
     * 用于动态调整策略（如：失败重试、权重调整）
     * @param address 被调用的服务地址
     * @param serviceName 服务名称
     * @param success 调用是否成功
     * @param responseTime 响应时间(毫秒)
     */
    default void notifyResult(InetSocketAddress address, String serviceName, boolean success, long responseTime) {
        // 默认实现为空，策略可选择性实现
    }
}
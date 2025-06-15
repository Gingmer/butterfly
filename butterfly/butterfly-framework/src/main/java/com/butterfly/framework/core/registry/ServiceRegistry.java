package com.butterfly.framework.core.registry;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * 企业级服务注册中心接口
 * 定义服务注册、发现、注销的标准契约
 */
public interface ServiceRegistry {
    /**
     * 注册服务
     * @param serviceName 服务名称 (格式: 接口全限定名:版本:分组)
     * @param serviceAddress 服务地址
     * @param metadata 服务元数据 (权重、健康检查URL等)
     */
    void register(String serviceName, InetSocketAddress serviceAddress, Map<String, String> metadata);

    /**
     * 注销服务
     * @param serviceName 服务名称
     * @param serviceAddress 服务地址
     */
    void unregister(String serviceName, InetSocketAddress serviceAddress);

    /**
     * 发现服务
     * @param serviceName 服务名称
     * @return 可用服务地址列表
     */
    List<InetSocketAddress> discover(String serviceName);

    /**
     * 订阅服务变更
     * @param serviceName 服务名称
     * @param listener 变更监听器
     */
    void subscribe(String serviceName, ServiceChangeListener listener);

    /**
     * 取消订阅服务变更
     * @param serviceName 服务名称
     * @param listener 变更监听器
     */
    void unsubscribe(String serviceName, ServiceChangeListener listener);

    /**
     * 服务变更监听器
     */
    /**
     * 获取服务元数据
     * @param serviceName 服务名称
     * @param address 服务地址
     * @return 服务元数据
     */
    Map<String, String> getServiceMetadata(String serviceName, InetSocketAddress address);

    interface ServiceChangeListener {
        /**
         * 当服务实例列表变更时触发
         * @param serviceName 服务名称
         * @param newServiceAddresses 新的服务地址列表
         */
        void onServiceChanged(String serviceName, List<InetSocketAddress> newServiceAddresses);
    }
}
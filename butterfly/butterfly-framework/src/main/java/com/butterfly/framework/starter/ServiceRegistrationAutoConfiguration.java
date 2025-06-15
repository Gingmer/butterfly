package com.butterfly.framework.starter;

import com.butterfly.framework.annotation.RpcService;
import com.butterfly.framework.config.NettyServerConfig;
import com.butterfly.framework.core.registry.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 企业级服务注册自动配置
 * 实现Spring Boot自动配置，完成服务启动时自动注册、关闭时自动注销
 */
@Component
@EnableConfigurationProperties(NettyServerConfig.class)
public class ServiceRegistrationAutoConfiguration implements ApplicationContextAware, SmartLifecycle {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistrationAutoConfiguration.class);
    private static final int PHASE = Integer.MAX_VALUE - 100;

    private final ServiceRegistry serviceRegistry;
    private final NettyServerConfig nettyServerConfig;
    private ApplicationContext applicationContext;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Autowired
    public ServiceRegistrationAutoConfiguration(ServiceRegistry serviceRegistry, NettyServerConfig nettyServerConfig) {
        this.serviceRegistry = serviceRegistry;
        this.nettyServerConfig = nettyServerConfig;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 应用启动完成后注册所有@RpcService服务
     */
    @Override
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                // 获取本机IP地址
                String hostAddress = InetAddress.getLocalHost().getHostAddress();
                int port = nettyServerConfig.getPort();
                InetSocketAddress serviceAddress = new InetSocketAddress(hostAddress, port);

                // 查找所有@RpcService注解的Bean
                Map<String, Object> rpcServiceBeans = applicationContext.getBeansWithAnnotation(RpcService.class);
                if (rpcServiceBeans.isEmpty()) {
                    logger.info("未发现@RpcService注解的服务Bean");
                    return;
                }

                // 注册每个服务
                for (Object serviceBean : rpcServiceBeans.values()) {
                    registerService(serviceBean, serviceAddress);
                }

                logger.info("服务注册完成，共注册{}个服务", rpcServiceBeans.size());
            } catch (UnknownHostException e) {
                logger.error("获取本机IP地址失败", e);
            }
        }
    }

    /**
     * 注册单个服务
     */
    private void registerService(Object serviceBean, InetSocketAddress serviceAddress) {
        Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
        RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);

        // 处理直接注解在类上的情况
        if (rpcService != null) {
            Class<?> interfaceClass = rpcService.interfaceClass() == void.class ? interfaces[0] : rpcService.interfaceClass();
            registerService(interfaceClass, rpcService, serviceAddress);
            return;
        }

        // 处理注解在接口上的情况
        for (Class<?> iface : interfaces) {
            rpcService = iface.getAnnotation(RpcService.class);
            if (rpcService != null) {
                registerService(iface, rpcService, serviceAddress);
            }
        }
    }

    /**
     * 执行服务注册
     */
    private void registerService(Class<?> interfaceClass, RpcService rpcService, InetSocketAddress serviceAddress) {
        String serviceName = String.format("%s:%s:%s",
                interfaceClass.getName(),
                rpcService.version(),
                rpcService.group());

        // 构建服务元数据
        Map<String, String> metadata = new HashMap<>();
        metadata.put("version", rpcService.version());
        metadata.put("group", rpcService.group());
        metadata.put("timeout", String.valueOf(rpcService.timeout()));
        metadata.put("ip", serviceAddress.getHostString());
        metadata.put("port", String.valueOf(serviceAddress.getPort()));

        // 注册服务
        serviceRegistry.register(serviceName, serviceAddress, metadata);
        logger.info("服务注册成功: {} -> {}", serviceName, serviceAddress);
    }

    /**
     * 应用关闭时注销所有服务
     */
    @Override
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            try {
                String hostAddress = InetAddress.getLocalHost().getHostAddress();
                int port = nettyServerConfig.getPort();
                InetSocketAddress serviceAddress = new InetSocketAddress(hostAddress, port);

                // 查找所有@RpcService注解的Bean
                Map<String, Object> rpcServiceBeans = applicationContext.getBeansWithAnnotation(RpcService.class);
                for (Object serviceBean : rpcServiceBeans.values()) {
                    unregisterService(serviceBean, serviceAddress);
                }

                logger.info("服务注销完成");
            } catch (UnknownHostException e) {
                logger.error("获取本机IP地址失败", e);
            }
        }
    }

    /**
     * 注销单个服务
     */
    private void unregisterService(Object serviceBean, InetSocketAddress serviceAddress) {
        Class<?>[] interfaces = serviceBean.getClass().getInterfaces();
        RpcService rpcService = serviceBean.getClass().getAnnotation(RpcService.class);

        // 处理直接注解在类上的情况
        if (rpcService != null) {
            Class<?> interfaceClass = rpcService.interfaceClass() == void.class ? interfaces[0] : rpcService.interfaceClass();
            String serviceName = buildServiceName(interfaceClass, rpcService);
            serviceRegistry.unregister(serviceName, serviceAddress);
            return;
        }

        // 处理注解在接口上的情况
        for (Class<?> iface : interfaces) {
            rpcService = iface.getAnnotation(RpcService.class);
            if (rpcService != null) {
                String serviceName = buildServiceName(iface, rpcService);
                serviceRegistry.unregister(serviceName, serviceAddress);
            }
        }
    }

    /**
     * 构建服务名称
     */
    private String buildServiceName(Class<?> interfaceClass, RpcService rpcService) {
        return String.format("%s:%s:%s",
                interfaceClass.getName(),
                rpcService.version(),
                rpcService.group());
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public int getPhase() {
        return PHASE; // 确保在Netty服务器启动之后再注册服务
    }
}
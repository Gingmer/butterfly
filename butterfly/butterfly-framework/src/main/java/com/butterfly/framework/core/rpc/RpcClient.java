package com.butterfly.framework.core.rpc;

import com.alibaba.fastjson.JSON;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import com.butterfly.framework.config.NettyClientConfig;
import com.butterfly.framework.core.registry.ServiceDiscovery;
import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PreDestroy;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 企业级RPC客户端实现
 * 负责与服务端建立连接并发送RPC请求
 */
@Component
public class RpcClient {
    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);
    @Value("${rpc.client.timeout:3000}")
    private int defaultTimeout;

    private final EventLoopGroup eventLoopGroup;
    private final Bootstrap bootstrap;
    private ChannelFuture channelFuture;
    private final ConcurrentHashMap<String, CompletableFuture<RpcResponse>> requestFutureMap;
    private final ConcurrentHashMap<String, InetSocketAddress> serviceAddressCache;

    @Autowired
    private NettyClientConfig nettyClientConfig;

    @Autowired
    private ServiceDiscovery serviceDiscovery;

    public RpcClient() {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.requestFutureMap = new ConcurrentHashMap<>();
        this.serviceAddressCache = new ConcurrentHashMap<>();
        initClient();
    }

    /**
     * 初始化Netty客户端
     */
    private void initClient() {
        try {
            bootstrap.group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis())
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(
                                            nettyClientConfig.getMaxFrameLength(),
                                            0, 4, 0, 4))
                                    .addLast(new LengthFieldPrepender(4))
                                    .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                    .addLast(new StringEncoder(CharsetUtil.UTF_8))
                                    .addLast(new RpcClientHandler(this, requestFutureMap));
                        }
                    });

            logger.info("Netty RPC客户端初始化完成");
        } catch (Exception e) {
            logger.error("Netty RPC客户端初始化失败", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 获取服务地址并建立连接
     */
    private ChannelFuture getChannelFuture(String serviceName) throws InterruptedException {
        // 从缓存获取服务地址
        InetSocketAddress serviceAddress = serviceAddressCache.get(serviceName);

        // 如果缓存中没有地址或连接已关闭，重新发现服务
        if (serviceAddress == null || (channelFuture != null && !channelFuture.channel().isActive())) {
            // 使用服务发现获取服务地址
            serviceAddress = serviceDiscovery.discoverService(serviceName);
            if (serviceAddress == null) {
                throw new IllegalStateException("无法发现服务: " + serviceName);
            }

            logger.info("发现服务地址: {} -> {}", serviceName, serviceAddress);
            serviceAddressCache.put(serviceName, serviceAddress);

            // 建立新连接
            if (channelFuture != null) {
                channelFuture.channel().close();
            }
            channelFuture = bootstrap.connect(serviceAddress.getHostString(), serviceAddress.getPort()).sync();
            logger.info("已连接到服务: {} -> {}:{}",
                    serviceName, serviceAddress.getHostString(), serviceAddress.getPort());
        }

        return channelFuture;
    }

    /**
     * 发送RPC请求
     * @param serviceName 服务名称
     * @param methodName 方法名称
     * @param parameterTypes 参数类型
     * @param parameters 参数值
     * @return 异步结果
     */
    public CompletableFuture<RpcResponse> sendRequest(String serviceName, String methodName, 
                                                     Class<?>[] parameterTypes, Object[] parameters) {
        // 生成唯一请求ID
        String requestId = UUID.randomUUID().toString();

        // 创建请求对象
        RpcRequest request = new RpcRequest();
        request.setRequestId(requestId);
        request.setServiceName(serviceName);
        request.setMethodName(methodName);
        request.setParameterTypes(parameterTypes);
        request.setParameters(parameters);

        // 创建异步结果对象
        CompletableFuture<RpcResponse> future = new CompletableFuture<>();
        requestFutureMap.put(requestId, future);

        try {
            // 获取服务地址并发送请求
            ChannelFuture channelFuture = getChannelFuture(serviceName);
            channelFuture.channel().writeAndFlush(JSON.toJSONString(request));
            logger.info("已发送RPC请求: {} -> {}", requestId, serviceName);

            // 设置超时处理
            channelFuture.orTimeout(defaultTimeout, TimeUnit.MILLISECONDS)
                    .whenComplete((response, ex) -> {
                        requestFutureMap.remove(requestId);
                        if (ex != null) {
                            logger.error("RPC请求超时: {}", requestId, ex);
                        }
                    });

            return future;
        } catch (Exception e) {
            requestFutureMap.remove(requestId);
            logger.error("发送RPC请求失败", e);
            future.completeExceptionally(e);
            return future;
        }
    }

    /**
     * 重新连接所有服务
     */
    public void reconnect() throws InterruptedException {
        logger.info("开始重新连接所有服务...");
        serviceAddressCache.clear();
        if (channelFuture != null) {
            channelFuture.channel().close().sync();
        }
        // 尝试重新连接第一个服务
        if (!serviceAddressCache.isEmpty()) {
            String firstService = serviceAddressCache.keySet().iterator().next();
            getChannelFuture(firstService);
        }
        logger.info("服务重新连接完成");
    }

    /**
     * 优雅关闭客户端
     */
    @PreDestroy
    public void close() {
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        logger.info("Netty RPC客户端已关闭");
    }
}
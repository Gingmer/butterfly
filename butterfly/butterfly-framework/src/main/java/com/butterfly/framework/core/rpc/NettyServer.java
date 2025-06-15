package com.butterfly.framework.core.rpc;

import com.butterfly.framework.config.NettyServerConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 企业级Netty服务器实现
 * 负责启动Netty服务并管理其生命周期
 */
@Component
public class NettyServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);

    @Autowired
    private NettyServerConfig nettyServerConfig;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture serverChannelFuture;

    /**
     * 初始化并启动Netty服务器
     * 使用Spring的@PostConstruct注解确保在依赖注入完成后执行
     */
    @PostConstruct
    public void start() throws InterruptedException {
        // 初始化EventLoopGroup
        bossGroup = new NioEventLoopGroup(nettyServerConfig.getBossThreadCount());
        workerGroup = new NioEventLoopGroup(nettyServerConfig.getWorkerThreadCount());

        try {
            // 创建ServerBootstrap
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, nettyServerConfig.getBacklog())
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyServerConfig.getConnectTimeoutMillis())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 配置ChannelPipeline
                            ch.pipeline()
                                    // 基于长度的帧解码器，解决粘包拆包问题
                                    .addLast(new LengthFieldBasedFrameDecoder(
                                            nettyServerConfig.getMaxFrameLength(),
                                            0, 4, 0, 4))
                                    // 长度字段编码器
                                    .addLast(new LengthFieldPrepender(4))
                                    // 字符串编解码器
                                    .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                    .addLast(new StringEncoder(CharsetUtil.UTF_8))
                                    // RPC请求处理器
                                    .addLast(new RpcRequestHandler());
                        }
                    });

            // 绑定端口并启动服务器
            serverChannelFuture = bootstrap.bind(nettyServerConfig.getPort()).sync();
            logger.info("Netty RPC服务器已启动，监听端口: {}", nettyServerConfig.getPort());

            // 等待服务器关闭
            serverChannelFuture.channel().closeFuture().sync();
        } finally {
            // 优雅关闭EventLoopGroup
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    /**
     * 优雅关闭Netty服务器
     * 使用Spring的@PreDestroy注解确保在容器销毁前执行
     */
    @PreDestroy
    public void stop() {
        if (serverChannelFuture != null) {
            serverChannelFuture.channel().close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        logger.info("Netty RPC服务器已关闭");
    }
}
package com.butterfly.framework.core.rpc;

import com.alibaba.fastjson.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 企业级RPC客户端处理器
 * 负责接收服务端响应并完成对应的异步请求
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long INITIAL_RECONNECT_DELAY = 1000;
    private static final double RECONNECT_DELAY_MULTIPLIER = 2.0;
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);
    private final RpcClient rpcClient;
    private final Map<String, CompletableFuture<RpcResponse>> requestFutureMap;

    public RpcClientHandler(RpcClient rpcClient, Map<String, CompletableFuture<RpcResponse>> requestFutureMap) {
        this.rpcClient = rpcClient;
        this.requestFutureMap = requestFutureMap;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String responseJson) {
        try {
            logger.info("收到RPC响应: {}", responseJson);
            RpcResponse response = JSON.parseObject(responseJson, RpcResponse.class);
            String requestId = response.getRequestId();

            // 从请求映射中获取对应的Future并完成
            CompletableFuture<RpcResponse> future = requestFutureMap.get(requestId);
            if (future != null) {
                if (response.isSuccess()) {
                    future.complete(response);
                } else {
                    future.completeExceptionally(new RuntimeException(
                        String.format("RPC调用失败 [code=%d]: %s", 
                        response.getErrorCode(), response.getErrorMessage())));
                }
            } else {
                logger.warn("未找到对应的请求ID: {}", requestId);
            }
        } catch (Exception e) {
            logger.error("解析RPC响应异常", e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("RPC客户端处理器异常", cause);
        // 异常时完成所有未完成的Future
        requestFutureMap.values().forEach(future ->
            future.completeExceptionally(new RuntimeException("RPC连接异常", cause)));
        requestFutureMap.clear();
        ctx.close();
    }

    @Override
public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.warn("RPC连接已断开，尝试重连...");
    int attempt = reconnectAttempts.incrementAndGet();
    if (attempt <= MAX_RECONNECT_ATTEMPTS) {
        long delay = (long) (INITIAL_RECONNECT_DELAY * Math.pow(RECONNECT_DELAY_MULTIPLIER, attempt - 1));
        logger.info("第{}次重连尝试，延迟{}ms...", attempt, delay);
        ctx.channel().eventLoop().schedule(() -> {
            try {
                // 调用RpcClient的重新连接方法
                rpcClient.reconnect();
                reconnectAttempts.set(0); // 重置重连计数器
            } catch (Exception e) {
                logger.error("重连失败", e);
                if (reconnectAttempts.get() >= MAX_RECONNECT_ATTEMPTS) {
                    logger.error("达到最大重连次数{}，停止尝试", MAX_RECONNECT_ATTEMPTS);
                    // 通知所有等待中的请求
                    requestFutureMap.values().forEach(future ->
                        future.completeExceptionally(new RuntimeException("RPC连接已断开且重连失败")));
                    requestFutureMap.clear();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    } else {
        logger.error("达到最大重连次数{}，停止尝试", MAX_RECONNECT_ATTEMPTS);
    }
    super.channelInactive(ctx);
}
}
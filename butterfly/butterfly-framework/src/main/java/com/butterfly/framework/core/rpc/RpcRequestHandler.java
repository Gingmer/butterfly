package com.butterfly.framework.core.rpc;


import com.alibaba.fastjson2.JSON;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 企业级RPC请求处理器
 * 负责解析RPC请求、执行相应服务方法并返回结果
 */
public class RpcRequestHandler extends SimpleChannelInboundHandler<String> {
    private static final Logger logger = LoggerFactory.getLogger(RpcRequestHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String requestJson) throws Exception {
        try {
            logger.info("收到RPC请求: {}", requestJson);

            // 反序列化请求
            RpcRequest request = JSON.parseObject(requestJson, RpcRequest.class);

            // 执行服务调用 (实际实现中应使用服务注册表和反射调用)
            Object result = invokeService(request);

            // 构建响应
            RpcResponse response = new RpcResponse();
            response.setRequestId(request.getRequestId());
            response.setResult(result);
            response.setSuccess(true);

            // 发送响应
            ctx.writeAndFlush(JSON.toJSONString(response));
        } catch (Exception e) {
            logger.error("处理RPC请求异常", e);
            RpcResponse errorResponse = new RpcResponse();
            errorResponse.setSuccess(false);
            errorResponse.setErrorMessage(e.getMessage());
            ctx.writeAndFlush(JSON.toJSONString(errorResponse));
        }
    }

    /**
     * 服务调用实现
     * 企业级实现中应包含服务发现、负载均衡和反射调用逻辑
     */
    private Object invokeService(RpcRequest request) {
        // 此处为简化实现，实际企业级代码应:
        // 1. 从服务注册表获取服务实例
        // 2. 根据负载均衡策略选择具体服务节点
        // 3. 使用反射机制调用目标方法
        // 4. 处理方法调用异常
        return "模拟调用服务: " + request.getServiceName() + ", 方法: " + request.getMethodName();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("RPC请求处理异常", cause);
        ctx.close();
    }
}
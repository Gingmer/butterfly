package com.butterfly.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业级Netty服务器配置类
 * 支持端口、线程数等核心参数的外部化配置
 */
@Component
@ConfigurationProperties(prefix = "netty.server")
public class NettyServerConfig {
    /** 服务器端口 */
    private int port = 8888;
    /** Boss线程组数量 */
    private int bossThreadCount = 1;
    /** Worker线程组数量 */
    private int workerThreadCount = Runtime.getRuntime().availableProcessors() * 2;
    /** TCP连接队列大小 */
    private int backlog = 1024;
    /** 连接超时时间(毫秒) */
    private int connectTimeoutMillis = 3000;
    /** 最大帧长度 */
    private int maxFrameLength = 1024 * 1024;

    // Getters and Setters
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public int getBossThreadCount() { return bossThreadCount; }
    public void setBossThreadCount(int bossThreadCount) { this.bossThreadCount = bossThreadCount; }
    public int getWorkerThreadCount() { return workerThreadCount; }
    public void setWorkerThreadCount(int workerThreadCount) { this.workerThreadCount = workerThreadCount; }
    public int getBacklog() { return backlog; }
    public void setBacklog(int backlog) { this.backlog = backlog; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public int getMaxFrameLength() { return maxFrameLength; }
    public void setMaxFrameLength(int maxFrameLength) { this.maxFrameLength = maxFrameLength; }
}
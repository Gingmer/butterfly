package com.butterfly.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 企业级Netty客户端配置类
 * 支持服务端地址、端口等核心连接参数的外部化配置
 */
@Component
@ConfigurationProperties(prefix = "netty.client")
public class NettyClientConfig {
    /** 服务端地址 */
    private String serverAddress = "localhost";
    /** 服务端端口 */
    private int serverPort = 8888;
    /** 连接超时时间(毫秒) */
    private int connectTimeoutMillis = 3000;
    /** 最大帧长度 */
    private int maxFrameLength = 1024 * 1024;
    /** 重连间隔(毫秒) */
    private int reconnectInterval = 5000;
    /** 是否启用SSL */
    private boolean sslEnabled = false;

    // Getters and Setters
    public String getServerAddress() { return serverAddress; }
    public void setServerAddress(String serverAddress) { this.serverAddress = serverAddress; }
    public int getServerPort() { return serverPort; }
    public void setServerPort(int serverPort) { this.serverPort = serverPort; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public int getMaxFrameLength() { return maxFrameLength; }
    public void setMaxFrameLength(int maxFrameLength) { this.maxFrameLength = maxFrameLength; }
    public int getReconnectInterval() { return reconnectInterval; }
    public void setReconnectInterval(int reconnectInterval) { this.reconnectInterval = reconnectInterval; }
    public boolean isSslEnabled() { return sslEnabled; }
    public void setSslEnabled(boolean sslEnabled) { this.sslEnabled = sslEnabled; }
}
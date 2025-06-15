package com.butterfly.framework.core.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * 企业级TCP健康检查实现
 * 通过尝试建立TCP连接验证服务实例的可用性
 */
@Component
public class TcpHealthChecker implements HealthChecker {
    private static final Logger logger = LoggerFactory.getLogger(TcpHealthChecker.class);
    private static final int DEFAULT_TIMEOUT = 3000;
    private static final int DEFAULT_INTERVAL = 10000;

    private int timeoutMillis = DEFAULT_TIMEOUT;
    private int intervalMillis = DEFAULT_INTERVAL;

    @Override
    public boolean check(String serviceName, InetSocketAddress address) {
        if (address == null) {
            logger.warn("服务地址为空，无法执行健康检查: {}", serviceName);
            return false;
        }

        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(address, timeoutMillis);
            boolean isAlive = socket.isConnected();
            logger.debug("服务健康检查结果: {} -> {}:{}, 状态: {}",
                    serviceName, address.getHostString(), address.getPort(), isAlive ? "健康" : "不健康");
            return isAlive;
        } catch (Exception e) {
            logger.warn("服务健康检查失败: {} -> {}:{}, 原因: {}",
                    serviceName, address.getHostString(), address.getPort(), e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
        }
    }

    @Override
    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        if (timeoutMillis > 0) {
            this.timeoutMillis = timeoutMillis;
        }
    }

    @Override
    public int getIntervalMillis() {
        return intervalMillis;
    }

    public void setIntervalMillis(int intervalMillis) {
        if (intervalMillis > 0) {
            this.intervalMillis = intervalMillis;
        }
    }
}
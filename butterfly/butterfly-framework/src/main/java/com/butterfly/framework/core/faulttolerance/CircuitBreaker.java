package com.butterfly.framework.core.faulttolerance;

/**
 * 熔断器接口
 * 实现服务调用的熔断模式，防止故障级联传播
 */
public interface CircuitBreaker {
    /**
     * 尝试执行目标方法
     * @param method 要执行的方法
     * @param <T> 返回值类型
     * @return 方法执行结果
     * @throws Exception 执行异常
     */
    <T> T execute(CircuitBreakerMethod<T> method) throws Exception;

    /**
     * 获取当前熔断器状态
     * @return 熔断器状态
     */
    CircuitBreakerState getState();

    /**
     * 重置熔断器
     */
    void reset();

    /**
     * 熔断器状态枚举
     */
    enum CircuitBreakerState {
        /**
         * 闭合状态：正常允许请求
         */
        CLOSED,
        /**
         * 打开状态：拒绝所有请求
         */
        OPEN,
        /**
         * 半开状态：允许部分请求试探
         */
        HALF_OPEN
    }

    /**
     * 熔断方法接口
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    interface CircuitBreakerMethod<T> {
        T invoke() throws Exception;
    }
}
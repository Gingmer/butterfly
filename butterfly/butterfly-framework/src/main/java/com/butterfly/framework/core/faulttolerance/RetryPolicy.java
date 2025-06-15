package com.butterfly.framework.core.faulttolerance;

/**
 * 重试策略接口
 * 定义服务调用失败后的重试机制
 */
public interface RetryPolicy {
    /**
     * 执行带重试的方法调用
     * @param method 要执行的方法
     * @param <T> 返回值类型
     * @return 方法执行结果
     * @throws Exception 重试耗尽后抛出的异常
     */
    <T> T execute(CircuitBreaker.CircuitBreakerMethod<T> method) throws Exception;

    /**
     * 获取当前重试次数
     * @return 当前重试次数
     */
    int getCurrentRetryCount();

    /**
     * 重置重试计数器
     */
    void reset();
}
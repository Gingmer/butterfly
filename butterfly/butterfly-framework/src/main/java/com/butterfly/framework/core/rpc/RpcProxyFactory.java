package com.butterfly.framework.core.rpc;

import com.alibaba.fastjson.JSON;
import com.butterfly.framework.annotation.RpcService;
import com.butterfly.framework.core.faulttolerance.CircuitBreaker;
import com.butterfly.framework.core.faulttolerance.DefaultCircuitBreaker;
import com.butterfly.framework.core.faulttolerance.ExponentialBackoffRetryPolicy;
import com.butterfly.framework.core.faulttolerance.RetryPolicy;
import com.butterfly.framework.core.config.RetryPolicyProperties;
import com.butterfly.framework.core.config.CircuitBreakerProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import com.alibaba.fastjson.JSON;

/**
 * RPC服务代理工厂，用于创建带有熔断和重试机制的服务代理
 */
@Component
public class RpcProxyFactory implements BeanPostProcessor {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RpcProxyFactory.class);

    private final RpcClient rpcClient;
    private final RetryPolicy retryPolicy;
    private final CircuitBreakerProperties circuitBreakerProperties;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    @Autowired
    public RpcProxyFactory(RpcClient rpcClient, RetryPolicyProperties retryPolicyProperties, CircuitBreakerProperties circuitBreakerProperties) {
        this.rpcClient = rpcClient;
        this.retryPolicy = new ExponentialBackoffRetryPolicy(retryPolicyProperties);
        this.circuitBreakerProperties = circuitBreakerProperties;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        // 只为标记了@RpcService注解的接口创建代理
        if (beanClass.isAnnotationPresent(RpcService.class)) {
            return createProxy(beanClass);
        }
        return bean;
    }

    /**
     * 创建JDK动态代理
     */
    private Object createProxy(Class<?> targetClass) {
        Class<?>[] interfaces = targetClass.getInterfaces();
        return Proxy.newProxyInstance(
                targetClass.getClassLoader(),
                interfaces,
                new RpcInvocationHandler()
        );
    }

    /**
     * RPC调用处理器，实现熔断和重试逻辑
     */
    private class RpcInvocationHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodKey = method.getDeclaringClass().getName() + "." + method.getName();
            CircuitBreaker circuitBreaker = getOrCreateCircuitBreaker(methodKey);

            try {
                // 执行重试逻辑，重试逻辑内部包含熔断逻辑
                CircuitBreaker.CircuitBreakerMethod<Object> task = () -> executeWithCircuitBreaker(circuitBreaker, method, args);
                    return retryPolicy.<Object>execute(task);
            } catch (CompletionException e) {
                throw e.getCause();
            } catch (Exception e) {
                log.error("RPC调用失败: {}", methodKey, e);
                throw e;
            }
        }

        /**
         * 获取或创建熔断实例
         */
        private CircuitBreaker getOrCreateCircuitBreaker(String methodKey) {
            return circuitBreakers.computeIfAbsent(methodKey, key -> {
                log.info("为方法[{}]创建新的熔断实例", key);
                return new DefaultCircuitBreaker(methodKey,
                        circuitBreakerProperties.getFailureThreshold(),
                        circuitBreakerProperties.getResetTimeoutMillis(),
                        circuitBreakerProperties.getRequestVolumeThreshold(),
                        circuitBreakerProperties.getHalfOpenMaxAttempts());
            });
        }

        /**
         * 执行带熔断机制的RPC调用
         */
        private Object executeWithCircuitBreaker(CircuitBreaker circuitBreaker, Method method, Object[] args) throws Exception {
            return circuitBreaker.execute(() -> {
                try {
                    // 执行实际的RPC调用
                    return rpcClient.sendRequest(method.getDeclaringClass().getName(), method.getName(), method.getParameterTypes(), args)
                        .thenApply(rpcResponse -> {
                            if (rpcResponse.isSuccess()) {
                                return JSON.parseObject(rpcResponse.getResult(), method.getReturnType());
                            } else {
                                throw new RuntimeException(rpcResponse.getError());
                            }
                        }).join();
                } catch (Exception e) {
                    log.error("RPC调用异常，触发熔断计数", e);
                    throw e;
                }
            });
        }
    }
}
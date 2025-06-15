package com.butterfly.framework.annotation;

import java.lang.annotation.*;

/**
 * 负载均衡策略注解
 * 用于标记自定义负载均衡策略实现类，并指定策略名称
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LoadBalanceStrategy {
    /**
     * 负载均衡策略名称
     */
    String value();
}
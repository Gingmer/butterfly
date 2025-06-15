package com.butterfly.framework.annotation;

import java.lang.annotation.*;

/**
 * 企业级RPC服务注解
 * 标记接口为远程服务，框架将自动生成代理并注册服务
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RpcService {
    /**
     * 服务接口类
     * 默认为当前注解标记的接口
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务版本号
     * 用于处理服务接口的兼容性问题
     */
    String version() default "1.0.0";

    /**
     * 服务分组
     * 用于区分同一接口的不同实现
     */
    String group() default "default";

    /**
     * 超时时间(毫秒)
     * 服务调用的默认超时时间
     */
    int timeout() default 3000;
}
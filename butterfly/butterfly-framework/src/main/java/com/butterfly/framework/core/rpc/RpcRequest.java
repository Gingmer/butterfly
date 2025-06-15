package com.butterfly.framework.core.rpc;

import java.io.Serializable;

/**
 * 企业级RPC请求模型
 * 定义RPC调用的标准请求格式，包含服务标识、方法信息和参数
 */
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 请求唯一标识，用于异步通信时关联请求与响应 */
    private String requestId;
    /** 服务接口名称 (全限定类名) */
    private String serviceName;
    /** 方法名称 */
    private String methodName;
    /** 参数类型列表 */
    private Class<?>[] parameterTypes;
    /** 参数值列表 */
    private Object[] parameters;

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public Class<?>[] getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(Class<?>[] parameterTypes) { this.parameterTypes = parameterTypes; }
    public Object[] getParameters() { return parameters; }
    public void setParameters(Object[] parameters) { this.parameters = parameters; }
}
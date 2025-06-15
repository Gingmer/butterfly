package com.butterfly.framework.core.rpc;

import java.io.Serializable;

/**
 * 企业级RPC响应模型
 * 定义RPC调用的标准响应格式，包含结果数据、状态标识和错误信息
 */
public class RpcResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 关联的请求ID，用于请求-响应匹配 */
    private String requestId;
    /** 调用结果数据 */
    private Object result;
    /** 调用是否成功 */
    private boolean success;
    /** 错误消息 (调用失败时非空) */
    private String errorMessage;
    /** 错误代码 (企业级错误码体系) */
    private int errorCode;
    /** 响应时间戳 */
    private long timestamp = System.currentTimeMillis();

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public int getErrorCode() { return errorCode; }
    public void setErrorCode(int errorCode) { this.errorCode = errorCode; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
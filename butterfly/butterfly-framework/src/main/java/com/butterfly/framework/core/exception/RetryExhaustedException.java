package com.butterfly.framework.core.exception;

/**
 * @author caisong_V
 * @version 1.0
 * @date 2025/6/15 20:15
 */
public class RetryExhaustedException extends RuntimeException{

    public RetryExhaustedException(String s, Throwable throwable) {
        super(s,throwable);
    }
}

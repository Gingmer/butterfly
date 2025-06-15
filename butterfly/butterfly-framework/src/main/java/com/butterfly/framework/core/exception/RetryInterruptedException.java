package com.butterfly.framework.core.exception;

/**
 * @author caisong_V
 * @version 1.0
 * @date 2025/6/15 20:14
 */
public class RetryInterruptedException extends RuntimeException{
    public RetryInterruptedException(String s, InterruptedException e) {
        super(s,e);
    }
}

package com.butterfly.framework.core.exception;

/**
 * @author caisong_V
 * @version 1.0
 * @date 2025/6/15 20:39
 */
public class SerializationException extends RuntimeException{
    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

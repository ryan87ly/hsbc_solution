package com.hsbc.objectpool;

public class RingBufferOperationException extends RuntimeException {

    public RingBufferOperationException(Throwable cause) {
        super(cause);
    }

    public RingBufferOperationException(String message) {
        super(message);
    }
}

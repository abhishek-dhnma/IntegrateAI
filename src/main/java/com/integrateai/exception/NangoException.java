package com.integrateai.exception;

public class NangoException extends RuntimeException {
    private final int statusCode;

    public NangoException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public NangoException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

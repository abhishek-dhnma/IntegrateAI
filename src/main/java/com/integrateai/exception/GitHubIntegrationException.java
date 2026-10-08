package com.integrateai.exception;

public class GitHubIntegrationException extends RuntimeException {
    private final int statusCode;

    public GitHubIntegrationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

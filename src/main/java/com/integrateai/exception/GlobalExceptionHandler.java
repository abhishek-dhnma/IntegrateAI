package com.integrateai.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NangoException.class)
    public ResponseEntity<Map<String, Object>> handleNangoException(NangoException ex) {
        log.error("[NANGO_ERROR] Status {}: {}", ex.getStatusCode(), ex.getMessage());
        String userFriendlyMessage = (ex.getStatusCode() == 404 || (ex.getMessage() != null && ex.getMessage().contains("missing_environment")))
                ? "GitHub isn't connected yet. Connect your GitHub account to continue."
                : "Unable to reach GitHub right now. Please check your connection and try again.";
        return ResponseEntity.status(ex.getStatusCode() > 0 ? ex.getStatusCode() : 500)
                .body(Map.of(
                        "error", "Connection Error",
                        "message", userFriendlyMessage,
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(GitHubIntegrationException.class)
    public ResponseEntity<Map<String, Object>> handleGitHubException(GitHubIntegrationException ex) {
        log.error("[GITHUB_ERROR] Status {}: {}", ex.getStatusCode(), ex.getMessage());
        String userFriendlyMessage = (ex.getStatusCode() == 404)
                ? "The specified repository or issue could not be found on GitHub."
                : "GitHub couldn't complete that request right now. Please try again.";
        return ResponseEntity.status(ex.getStatusCode() > 0 ? ex.getStatusCode() : 500)
                .body(Map.of(
                        "error", "GitHub Request Error",
                        "message", userFriendlyMessage,
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        log.warn("[BAD_REQUEST] {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Invalid Argument",
                        "message", ex.getMessage(),
                        "timestamp", Instant.now().toString()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("[INTERNAL_ERROR] Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "error", "Internal Server Error",
                        "message", ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred.",
                        "timestamp", Instant.now().toString()
                ));
    }
}

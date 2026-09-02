package vn.edu.cnpm.projectsupport.integration.github;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

public class GitHubApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final boolean retryable;
    private final Long retryAfterSeconds;
    private final String correlationId;

    public GitHubApiException(
            HttpStatus status,
            String errorCode,
            boolean retryable,
            Long retryAfterSeconds,
            String message,
            Throwable cause) {
        super(message == null || message.isBlank()
                ? "GitHub API request failed"
                : message, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;
        String currentCorrelationId = MDC.get("correlationId");
        this.correlationId = currentCorrelationId == null || currentCorrelationId.isBlank()
                ? UUID.randomUUID().toString()
                : currentCorrelationId;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}

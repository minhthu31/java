package vn.edu.cnpm.projectsupport.integration.jira.exception;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

public class JiraApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final boolean retryable;
    private final Long retryAfterSeconds;
    private final String correlationId;

    public JiraApiException(HttpStatus status, String errorCode, boolean retryable, Long retryAfterSeconds) {
        this(status, errorCode, retryable, retryAfterSeconds, null, null);
    }

    public JiraApiException(HttpStatus status, String errorCode, boolean retryable, Long retryAfterSeconds, String message, Throwable cause) {
        super(message == null || message.isBlank() ? "Jira API request failed" : message, cause);

        this.status = status;
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.retryAfterSeconds = retryAfterSeconds;

        String currentCorrelationId = MDC.get("correlationId");

        this.correlationId = currentCorrelationId == null || currentCorrelationId.isBlank() ? UUID.randomUUID().toString(): currentCorrelationId;
    }

    public JiraApiException(HttpStatus status) {
        this(status,"JIRA_UNAVAILABLE", false, null);
    }

    public JiraApiException(HttpStatus status,Throwable cause) {
        this(status,"JIRA_UNAVAILABLE",true, null, null, cause);
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
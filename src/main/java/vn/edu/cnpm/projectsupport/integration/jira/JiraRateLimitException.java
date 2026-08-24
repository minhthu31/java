package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Duration;

public class JiraRateLimitException extends RuntimeException {
    private final Duration retryAfter;

    public JiraRateLimitException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() { return retryAfter; }
}

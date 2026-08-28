package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Duration;

import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

public class JiraRateLimitException
        extends JiraApiException {

    private final Duration retryAfter;

    public JiraRateLimitException(
            String message,
            Duration retryAfter) {

        super(
                HttpStatus.TOO_MANY_REQUESTS,
                "JIRA_RATE_LIMITED",
                true,
                retryAfter == null
                        ? null
                        : retryAfter.getSeconds(),
                message,
                null);

        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
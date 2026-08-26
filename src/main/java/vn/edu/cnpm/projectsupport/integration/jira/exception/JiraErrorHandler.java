package vn.edu.cnpm.projectsupport.integration.jira.exception;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;

public final class JiraErrorHandler {

    private JiraErrorHandler() {
    }

    public static JiraApiException fromStatus(int status) {
        return fromStatus(status, null);
    }

    public static JiraApiException fromStatus(int status, String retryAfterHeader) {
        if (status == 400) {
            return new JiraApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", false, null);
        }

        if (status == 401) {
            return new JiraApiException(HttpStatus.UNAUTHORIZED, "JIRA_AUTH_FAILED", false, null);
        }

        if (status == 403) {
            return new JiraApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", false, null);
        }

        if (status == 404) {
            return new JiraApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", false, null);
        }

        if (status == 409) {
            return new JiraApiException(HttpStatus.CONFLICT, "JIRA_CONFLICT", false, null);
        }

        if (status == 422) {
            return new JiraApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", false, null);
        }

        if (status == 429) {
            return new JiraApiException(HttpStatus.TOO_MANY_REQUESTS, "JIRA_RATE_LIMITED", true,  parseRetryAfter(retryAfterHeader));
        }

        if (status >= 500) {
            return new JiraApiException(HttpStatus.BAD_GATEWAY, "JIRA_UNAVAILABLE", true, null);
        }

        return new JiraApiException(HttpStatus.BAD_GATEWAY, "JIRA_UNAVAILABLE", false, null);
    }

    public static JiraApiException fromThrowable(Throwable cause) {
        if (cause instanceof TimeoutException || cause instanceof IOException || cause instanceof ResourceAccessException) {
            return new JiraApiException(HttpStatus.BAD_GATEWAY, "JIRA_UNAVAILABLE", true, null, "Jira provider is unavailable", cause);
        }

        return new JiraApiException(HttpStatus.BAD_GATEWAY, "JIRA_UNAVAILABLE", false, null, "Jira API request failed", cause);
    }

    private static Long parseRetryAfter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            long seconds = Long.parseLong(value.trim());
            return seconds >= 0 ? seconds : null;

        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
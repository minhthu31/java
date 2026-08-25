package vn.edu.cnpm.projectsupport.integration.jira.exception;

import org.springframework.http.HttpStatus;

public final class JiraErrorHandler {

    private JiraErrorHandler() {
    }

    public static JiraApiException fromStatus(int status) {

        if (status == 400) {
            return new JiraApiException(HttpStatus.BAD_REQUEST);
        }

        if (status == 401) {
            return new JiraApiException(HttpStatus.UNAUTHORIZED);
        }

        if (status == 403) {
            return new JiraApiException(HttpStatus.FORBIDDEN);
        }

        if (status == 404) {
            return new JiraApiException(HttpStatus.NOT_FOUND);
        }

        if (status == 409) {
            return new JiraApiException(HttpStatus.CONFLICT);
        }

        if (status == 429) {
            return new JiraApiException(HttpStatus.TOO_MANY_REQUESTS);
        }

        if (status >= 500) {
            return new JiraApiException(HttpStatus.BAD_GATEWAY);
        }

        return new JiraApiException(HttpStatus.BAD_GATEWAY);
    }
}
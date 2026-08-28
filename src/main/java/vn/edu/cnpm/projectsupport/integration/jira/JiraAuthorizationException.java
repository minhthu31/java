package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

public class JiraAuthorizationException
        extends JiraApiException {

    public JiraAuthorizationException(String message) {
        super(
                HttpStatus.FORBIDDEN,
                "JIRA_AUTHORIZATION_FAILED",
                false,
                null,
                message,
                null);
    }

    public JiraAuthorizationException(
            String message,
            Throwable cause) {

        super(
                HttpStatus.FORBIDDEN,
                "JIRA_AUTHORIZATION_FAILED",
                false,
                null,
                message,
                cause);
    }
}
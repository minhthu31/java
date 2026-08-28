package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

public class JiraAuthenticationException
        extends JiraApiException {

    public JiraAuthenticationException(String message) {
        super(
                HttpStatus.UNAUTHORIZED,
                "JIRA_AUTHENTICATION_FAILED",
                false,
                null,
                message,
                null);
    }

    public JiraAuthenticationException(
            String message,
            Throwable cause) {

        super(
                HttpStatus.UNAUTHORIZED,
                "JIRA_AUTHENTICATION_FAILED",
                false,
                null,
                message,
                cause);
    }
}
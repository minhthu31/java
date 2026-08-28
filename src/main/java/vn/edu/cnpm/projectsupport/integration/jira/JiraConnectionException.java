package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

public class JiraConnectionException
        extends JiraApiException {

    public JiraConnectionException(String message) {

        super(
                HttpStatus.BAD_GATEWAY,
                "JIRA_CONNECTION_FAILED",
                true,
                null,
                message,
                null);
    }

    public JiraConnectionException(
            String message,
            Throwable cause) {

        super(
                HttpStatus.BAD_GATEWAY,
                "JIRA_CONNECTION_FAILED",
                true,
                null,
                message,
                cause);
    }
}
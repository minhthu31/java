package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

public class JiraClientException
        extends JiraApiException {

    public JiraClientException(String message) {

        super(
                HttpStatus.BAD_REQUEST,
                "JIRA_REQUEST_FAILED",
                false,
                null,
                message,
                null);
    }

    public JiraClientException(
            String message,
            Throwable cause) {

        super(
                HttpStatus.BAD_REQUEST,
                "JIRA_REQUEST_FAILED",
                false,
                null,
                message,
                cause);
    }
}
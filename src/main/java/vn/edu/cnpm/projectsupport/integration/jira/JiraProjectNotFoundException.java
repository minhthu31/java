package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.http.HttpStatus;

import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

public class JiraProjectNotFoundException
        extends JiraApiException {

    public JiraProjectNotFoundException(String message) {
        super(
                HttpStatus.NOT_FOUND,
                "JIRA_RESOURCE_NOT_FOUND",
                false,
                null,
                message,
                null);
    }

    public JiraProjectNotFoundException(
            String message,
            Throwable cause) {

        super(
                HttpStatus.NOT_FOUND,
                "JIRA_RESOURCE_NOT_FOUND",
                false,
                null,
                message,
                cause);
    }
}
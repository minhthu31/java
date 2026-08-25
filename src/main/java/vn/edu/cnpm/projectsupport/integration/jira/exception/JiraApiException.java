package vn.edu.cnpm.projectsupport.integration.jira.exception;
import org.springframework.http.HttpStatus;

public class JiraApiException extends RuntimeException {

    private final HttpStatus status;

    public JiraApiException(HttpStatus status) {
        super("Jira API request failed");
        this.status = status;
    }

    public JiraApiException(HttpStatus status, Throwable cause) {
        super("Jira API request failed", cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
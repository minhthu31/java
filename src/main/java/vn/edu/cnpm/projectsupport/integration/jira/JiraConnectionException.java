package vn.edu.cnpm.projectsupport.integration.jira;

public class JiraConnectionException extends RuntimeException {
    public JiraConnectionException(String message) { super(message); }
    public JiraConnectionException(String message, Throwable cause) { super(message, cause); }
}

package vn.edu.cnpm.projectsupport.integration.jira;

public class JiraProjectNotFoundException extends RuntimeException {
    public JiraProjectNotFoundException(String message) { super(message); }
}

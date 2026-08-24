package vn.edu.cnpm.projectsupport.integration.jira;

public interface JiraClient {
    JiraConnectionResult testConnection();
    JiraProject getProject(String projectKey);
}

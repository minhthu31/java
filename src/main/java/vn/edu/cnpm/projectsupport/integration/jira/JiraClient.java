package vn.edu.cnpm.projectsupport.integration.jira;

public interface JiraClient {

    JiraConnectionResult testConnection(
            Long projectId,
            String projectKey);

    JiraProject getProject(
            Long projectId,
            String projectKey);
}
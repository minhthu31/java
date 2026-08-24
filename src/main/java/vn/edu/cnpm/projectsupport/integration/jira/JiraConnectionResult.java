package vn.edu.cnpm.projectsupport.integration.jira;

public record JiraConnectionResult(boolean connected, String projectKey, String projectName) {
}

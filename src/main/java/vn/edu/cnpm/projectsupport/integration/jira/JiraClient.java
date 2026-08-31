package vn.edu.cnpm.projectsupport.integration.jira;

import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueRequest;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueResponse;

public interface JiraClient {

    JiraConnectionResult testConnection(
            Long projectId,
            String projectKey);

    JiraProject getProject(
            Long projectId,
            String projectKey);

    JiraCreateIssueResponse createIssue(
            Long projectId,
            String projectKey,
            JiraCreateIssueRequest request);

    java.util.List<JiraCreateIssueResponse> findIssuesByLabel(
            Long projectId,
            String projectKey,
            String label);

    void updateIssue(
            Long projectId,
            String projectKey,
            String jiraIssueId,
            JiraCreateIssueRequest request);
}
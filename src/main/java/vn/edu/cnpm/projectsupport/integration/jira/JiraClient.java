package vn.edu.cnpm.projectsupport.integration.jira;

import java.util.List;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueRequest;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintPageDto;

public interface JiraClient {

    JiraConnectionResult testConnection(Long projectId, String projectKey);

    JiraProject getProject(Long projectId, String projectKey);

    JiraPageDto<JiraIssueDto> getIssues(Long projectId, String projectKey, int startAt, int maxResults);

    JiraPageDto<JiraIssueDto> getBacklog(Long projectId, String projectKey, int startAt, int maxResults);

    JiraSprintPageDto getSprints(Long projectId, String projectKey, int startAt, int maxResults);

    JiraCreateIssueResponse createIssue(
            Long projectId,
            String projectKey,
            JiraCreateIssueRequest request);

    List<JiraCreateIssueResponse> findIssuesByLabel(
            Long projectId,
            String projectKey,
            String label);

    void updateIssue(
            Long projectId,
            String projectKey,
            String jiraIssueId,
            JiraCreateIssueRequest request);

    void addIssueToSprint(
            Long projectId,
            String jiraSprintId,
            String jiraIssueId);

    void updateIssueAssignee(
            Long projectId,
            String projectKey,
            String jiraIssueKey,
            String assigneeAccountId);

    void transitionIssueStatus(
            Long projectId,
            String projectKey,
            String jiraIssueKey,
            String targetStatusName);
}
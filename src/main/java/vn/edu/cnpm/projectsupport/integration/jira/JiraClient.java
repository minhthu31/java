package vn.edu.cnpm.projectsupport.integration.jira;

<<<<<<< HEAD
public interface JiraClient {

    JiraConnectionResult testConnection(
            Long projectId,
            String projectKey);

    JiraProject getProject(
            Long projectId,
            String projectKey);
}
=======
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintPageDto;

public interface JiraClient {

    JiraConnectionResult testConnection(Long projectId, String projectKey);

    JiraProject getProject(Long projectId, String projectKey);

    JiraPageDto<JiraIssueDto> getIssues(Long projectId, String projectKey, int startAt, int maxResults);

    JiraPageDto<JiraIssueDto> getBacklog(Long projectId, String projectKey, int startAt, int maxResults);

    JiraSprintPageDto getSprints(Long projectId, String projectKey, int startAt, int maxResults);
}
>>>>>>> 6f00c2c (CNPM-81 implement Jira project issue backlog sprint sync)

package vn.edu.cnpm.projectsupport.integration.jira.dto;

public record JiraIssueDto(String id, String key, String summary, String description, JiraStatusDto status, 
        JiraPriorityDto priority, JiraUserDto assignee,JiraProjectDto project) {
}
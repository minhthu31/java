package vn.edu.cnpm.projectsupport.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraCreateIssueFieldsDto(
        JiraProjectDto project,
        String summary,
        JiraAdfDocumentDto description,
        JiraIssueTypeDto issuetype,
        JiraPriorityDto priority,
        JiraUserDto assignee) {
}
package vn.edu.cnpm.projectsupport.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueFieldsDto(
        String summary,
        JiraAdfDocumentDto description,
        JiraStatusDto status,
        JiraPriorityDto priority,
        JiraUserDto assignee,
        JiraProjectDto project,
        JiraIssueTypeDto issuetype,
        LocalDate duedate,
        JiraParentIssueDto parent,
        String updated) {
}
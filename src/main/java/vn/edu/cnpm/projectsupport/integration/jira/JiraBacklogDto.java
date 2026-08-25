package vn.edu.cnpm.projectsupport.integration.jira.dto;

import java.util.List;

public record JiraBacklogDto(List<JiraIssueDto> issues) {
    
}
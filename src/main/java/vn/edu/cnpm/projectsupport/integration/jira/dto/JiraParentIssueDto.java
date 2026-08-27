package vn.edu.cnpm.projectsupport.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraParentIssueDto(String id, String key) {
}
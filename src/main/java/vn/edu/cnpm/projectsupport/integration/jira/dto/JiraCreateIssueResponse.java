package vn.edu.cnpm.projectsupport.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraCreateIssueResponse(
String id,
String key,
String self) {
}

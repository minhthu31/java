package vn.edu.cnpm.projectsupport.integration.jira.dto;

public record JiraCreateIssueRequest(
String summary,
String description,
String issueType,
String priority) {
}

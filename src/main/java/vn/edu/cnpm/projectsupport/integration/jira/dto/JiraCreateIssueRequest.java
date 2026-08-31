package vn.edu.cnpm.projectsupport.integration.jira.dto;

import java.util.List;

public record JiraCreateIssueRequest(
        String summary,
        String description,
        String issueType,
        String priority,
        List<String> labels) {

    public JiraCreateIssueRequest(
            String summary,
            String description,
            String issueType,
            String priority) {
        this(summary, description, issueType, priority, List.of());
    }
}

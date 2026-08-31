package vn.edu.cnpm.projectsupport.integration.jira.dto;

import java.util.List;

public record JiraCreateIssueRequest(
        String summary,
        String description,
        String issueType,
        String priority,
        List<String> labels,
        String assigneeEmail,
        String dueDate,
        String sprintId,
        String epicKey) {

    public JiraCreateIssueRequest(
            String summary,
            String description,
            String issueType,
            String priority) {
        this(summary, description, issueType, priority, List.of(), null, null, null, null);
    }

    public JiraCreateIssueRequest(
            String summary,
            String description,
            String issueType,
            String priority,
            List<String> labels) {
        this(summary, description, issueType, priority, labels, null, null, null, null);
    }
}

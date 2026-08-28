package vn.edu.cnpm.projectsupport.integration.jira.dto;

public record JiraUserDto(
        String accountId,
        String displayName,
        String emailAddress,
        boolean active
){}
package vn.edu.cnpm.projectsupport.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraSprintPageDto(
        int startAt,
        int maxResults,
        int total,
        Boolean isLast,
        List<JiraSprintDto> values) {
}

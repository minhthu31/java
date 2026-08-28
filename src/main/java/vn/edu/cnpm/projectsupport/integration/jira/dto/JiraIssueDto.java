package vn.edu.cnpm.projectsupport.integration.jira.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssueDto(String id, String key, JiraIssueFieldsDto fields, String updated) {

    public JiraIssueDto(String id, String key, JiraIssueFieldsDto fields) {
        this(id, key, fields, null);
    }

    public String summary() {
        return fields == null ? null : fields.summary();
    }

    public JiraAdfDocumentDto description() {
        return fields == null ? null : fields.description();
    }

    public JiraStatusDto status() {
        return fields == null ? null : fields.status();
    }

    public JiraPriorityDto priority() {
        return fields == null ? null : fields.priority();
    }

    public JiraUserDto assignee() {
        return fields == null ? null : fields.assignee();
    }

    public JiraProjectDto project() {
        return fields == null ? null : fields.project();
    }
}

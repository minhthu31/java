package vn.edu.cnpm.projectsupport.integration.jira.mapper;

import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPriorityDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraProjectDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraStatusDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraUserDto;

public class JiraMapper {
    public JiraProjectDto toProject(String id, String key, String name) {
        return new JiraProjectDto(id, key, name);
    }

    public JiraUserDto toUser(String accountId, String displayName, String emailAddress, boolean active) {
        return new JiraUserDto(accountId, displayName, emailAddress, active);
    }

    public JiraStatusDto toStatus(String id, String name) {
        return new JiraStatusDto(id, name);
    }

    public JiraPriorityDto toPriority(String id, String name) {
        return new JiraPriorityDto(id, name);
    }

    public JiraIssueDto toIssue(String id, String key, String summary, String description, JiraStatusDto status,
                                JiraPriorityDto priority, JiraUserDto assignee, JiraProjectDto project) {
        return new JiraIssueDto(id, key, summary,description,status, priority, assignee, project);
    }
}
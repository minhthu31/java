package vn.edu.cnpm.projectsupport.integration.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPriorityDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraProjectDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraStatusDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraUserDto;
import vn.edu.cnpm.projectsupport.integration.jira.mapper.JiraMapper;

class JiraMapperTest {
    private final JiraMapper mapper = new JiraMapper();
    @Test
    void shouldMapIssue() {
        JiraProjectDto project =mapper.toProject("10000","CNPM","Project Support");
        JiraUserDto user = mapper.toUser("user-1","Nguyen Hue","quangtrung12@gmail.com",true);
        JiraStatusDto status = mapper.toStatus("3", "In Progress");
        JiraPriorityDto priority = mapper.toPriority("2", "High");
        JiraIssueDto issue = mapper.toIssue("10001", "CNPM-10", "Implement Jira DTO", "Create Jira DTO and mapper", status, priority, user, project);

        assertEquals("10001", issue.id());
        assertEquals("CNPM-10", issue.key());
        assertEquals("Implement Jira DTO", issue.summary());

        assertEquals("In Progress", issue.status().name());
        assertEquals("High", issue.priority().name());

        assertEquals("user-1", issue.assignee().accountId());
        assertEquals("CNPM", issue.project().key());
    }
}
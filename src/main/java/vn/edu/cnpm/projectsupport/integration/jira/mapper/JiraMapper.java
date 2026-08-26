package vn.edu.cnpm.projectsupport.integration.jira.mapper;

import java.util.List;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraAdfDocumentDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraAdfNodeDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueFieldsDto;
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

    public JiraIssueDto toIssue(String id, String key, String summary, JiraAdfDocumentDto description, JiraStatusDto status,
         JiraPriorityDto priority, JiraUserDto assignee, JiraProjectDto project) {
        JiraIssueFieldsDto fields = new JiraIssueFieldsDto(summary, description, status, priority, assignee, project, null, null, null);
        return new JiraIssueDto(id, key, fields);
    }

    public JiraIssueDto toIssue(String id, String key, String summary, String description, JiraStatusDto status, JiraPriorityDto priority, JiraUserDto assignee, JiraProjectDto project) {
        JiraAdfDocumentDto adf = description == null ? null : 
        new JiraAdfDocumentDto(1,"doc",List.of(new JiraAdfNodeDto("paragraph",null,null,null,List.of(new JiraAdfNodeDto("text", description,null,null,null)))));

        return toIssue(id,key,summary,adf,status,priority,assignee,project);
    }

    public String toPlainText(JiraAdfDocumentDto document) {
        if (document == null || document.content() == null) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        appendText(document.content(), text);
        return text.toString().trim();
    }

    private void appendText(
            List<JiraAdfNodeDto> nodes, StringBuilder text) {

        for (JiraAdfNodeDto node : nodes) {
            if (node.text() != null) {
                text.append(node.text());
            }

            if (node.content() != null) {
                appendText(node.content(), text);
            }

            if ("paragraph".equals(node.type())|| "heading".equals(node.type())) {
                text.append('\n');
            }
        }
    }
}
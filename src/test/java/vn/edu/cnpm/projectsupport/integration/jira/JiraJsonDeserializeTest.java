package vn.edu.cnpm.projectsupport.integration.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;

class JiraJsonDeserializeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeRealJiraIssueShapeWithFieldsAndAdfDescription()
            throws Exception {

        String json = """
                {
                  "id": "10001",
                  "key": "CNPM-10",
                  "fields": {
                    "summary": "Implement Jira DTO",
                    "updated": "2026-08-29T10:30:00.000+0700",
                    "description": {
                      "version": 1,
                      "type": "doc",
                      "content": [
                        {
                          "type": "paragraph",
                          "content": [
                            {
                              "type": "text",
                              "text": "Description from Jira"
                            }
                          ]
                        }
                      ]
                    },
                    "status": {
                      "id": "3",
                      "name": "In Progress"
                    },
                    "priority": {
                      "id": "2",
                      "name": "High"
                    },
                    "assignee": {
                      "accountId": "user-1",
                      "displayName": "Nguyen Hue",
                      "active": true
                    },
                    "project": {
                      "id": "10000",
                      "key": "CNPM",
                      "name": "Project Support"
                    },
                    "issuetype": {
                      "id": "10001",
                      "name": "Task"
                    }
                  }
                }
                """;

        JiraIssueDto issue = objectMapper.readValue(json, JiraIssueDto.class);

        assertEquals("10001",issue.id());
        assertEquals("CNPM-10",issue.key());
        assertEquals("Implement Jira DTO",issue.fields().summary());
        assertEquals("2026-08-29T10:30:00.000+0700",issue.updated());
        assertNotNull(issue.fields().description());
        assertEquals(1,issue.fields().description().version());
        assertEquals("doc",issue.fields().description().type());
        assertEquals("Description from Jira",issue.fields().description().content().get(0).content().get(0).text());
        assertEquals("In Progress", issue.fields().status().name());
        assertEquals("High", issue.fields().priority().name());
        assertEquals("user-1", issue.fields().assignee().accountId());
        assertEquals("CNPM", issue.fields().project().key());
        assertEquals("Task", issue.fields().issuetype().name());
    }
}
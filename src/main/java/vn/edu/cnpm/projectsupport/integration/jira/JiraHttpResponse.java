package vn.edu.cnpm.projectsupport.integration.jira;

import java.util.Map;

public record JiraHttpResponse(int statusCode, String body, Map<String, String> headers) {
}

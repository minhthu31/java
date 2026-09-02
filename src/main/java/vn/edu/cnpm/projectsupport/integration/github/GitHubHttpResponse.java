package vn.edu.cnpm.projectsupport.integration.github;

import java.util.Map;

public record GitHubHttpResponse(
        int statusCode,
        String body,
        Map<String, String> headers) {
}

package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/admin/integrations/jira")
@PreAuthorize("hasRole('ADMIN')")
public class JiraConnectionController {
    private final JiraClient jiraClient;

    public JiraConnectionController(JiraClient jiraClient) {
        this.jiraClient = jiraClient;
    }

    @PostMapping("/test-connection")
    public ResponseEntity<ApiResponse<JiraConnectionResult>> testConnection() {
        return ResponseEntity.ok(ApiResponse.success(jiraClient.testConnection()));
    }
}

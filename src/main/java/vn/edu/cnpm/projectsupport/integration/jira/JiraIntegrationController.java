package vn.edu.cnpm.projectsupport.integration.jira;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionRequest;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionTestResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/jira")
public class JiraIntegrationController {

    private final JiraIntegrationService jiraIntegrationService;

    public JiraIntegrationController(JiraIntegrationService jiraIntegrationService) {
        this.jiraIntegrationService = jiraIntegrationService;
    }

    /**
     * GET /api/v1/projects/{projectId}/integrations/jira/config
     * Đọc cấu hình Jira đã che thông tin bí mật.
     * Quyền: ADMIN hoặc TEAM_LEADER.
     */
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEAM_LEADER')")
    public ResponseEntity<Map<String, Object>> getJiraConnection(@PathVariable Long projectId) {
        JiraConnectionResponse response = jiraIntegrationService.getConnection(projectId);
        return ResponseEntity.ok(Map.of(
                "data", response,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * PUT /api/v1/projects/{projectId}/integrations/jira/config
     * Tạo hoặc thay thế cấu hình Jira của project.
     * Quyền: Chỉ ADMIN.
     */
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> configureJiraConnection(
            @PathVariable Long projectId,
            @Valid @RequestBody JiraConnectionRequest request) {
        JiraConnectionResponse response = jiraIntegrationService.configureConnection(projectId, request);
        return ResponseEntity.ok(Map.of(
                "data", response,
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * POST /api/v1/projects/{projectId}/integrations/jira/test-connection
     * Kiểm tra credential, user hiện tại và Jira project.
     * Quyền: Chỉ ADMIN.
     */
    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testJiraConnection(@PathVariable Long projectId) {
        JiraConnectionTestResponse response = jiraIntegrationService.testConnection(projectId);
        return ResponseEntity.ok(Map.of(
                "data", response,
                "timestamp", Instant.now().toString()
        ));
    }
}

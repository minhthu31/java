package vn.edu.cnpm.projectsupport.integration.jira;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
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

    @GetMapping("/config")
    // Điểm 5: Kiểm tra Team Leader phải thuộc đúng project
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEAM_LEADER') and @projectAuthorizationService.isMemberOfProject(#projectId))")
    public ResponseEntity<ApiResponse<JiraConnectionResponse>> getJiraConnection(@PathVariable Long projectId) {
        JiraConnectionResponse response = jiraIntegrationService.getConnection(projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<JiraConnectionResponse>> configureJiraConnection(
            @PathVariable Long projectId,
            @Valid @RequestBody JiraConnectionRequest request) {
        JiraConnectionResponse response = jiraIntegrationService.configureConnection(projectId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<JiraConnectionTestResponse>> testJiraConnection(@PathVariable Long projectId) {
        JiraConnectionTestResponse response = jiraIntegrationService.testConnection(projectId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
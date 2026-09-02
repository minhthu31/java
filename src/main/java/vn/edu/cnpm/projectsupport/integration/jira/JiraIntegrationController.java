
package vn.edu.cnpm.projectsupport.integration.jira;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionRequest;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionTestResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraTaskSyncResponse;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/jira")
public class JiraIntegrationController {

    private final JiraIntegrationService jiraIntegrationService;

    public JiraIntegrationController(
            JiraIntegrationService jiraIntegrationService) {

        this.jiraIntegrationService = jiraIntegrationService;
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEAM_LEADER') and @projectAuthorization.isCurrentUserLeader(#projectId))")
    public ResponseEntity<ApiResponse<JiraConnectionResponse>> getJiraConnection(
            @PathVariable Long projectId) {

        JiraConnectionResponse response =
                jiraIntegrationService.getConnection(projectId);

        return ResponseEntity.ok(
                ApiResponse.success(response));
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<JiraConnectionResponse>> configureJiraConnection(
            @PathVariable Long projectId,
            @Valid @RequestBody JiraConnectionRequest request) {

        JiraConnectionResponse response =
                jiraIntegrationService.configureConnection(
                        projectId,
                        request);

        return ResponseEntity.ok(
                ApiResponse.success(response));
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<JiraConnectionTestResponse>> testJiraConnection(
            @PathVariable Long projectId) {

        JiraConnectionTestResponse response =
                jiraIntegrationService.testConnection(projectId);

        return ResponseEntity.ok(
                ApiResponse.success(response));
    }

    @GetMapping("/issues/{jiraIssueKey}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEAM_LEADER') and @projectAuthorization.isCurrentUserLeader(#projectId))")
    public ResponseEntity<ApiResponse<JiraIssueResponse>> getJiraIssue(
            @PathVariable Long projectId,
            @PathVariable String jiraIssueKey) {

        JiraIssueResponse response =
                jiraIntegrationService.getIssue(projectId, jiraIssueKey);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /*
     * ============================================================
     * CNPM-80
     * Sync local Task -> Jira
     * ============================================================
     */

    @PostMapping("/tasks/{taskId}/sync")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEAM_LEADER') and @projectAuthorization.isCurrentUserLeader(#projectId))")
    public ResponseEntity<?> syncTaskToJira(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestHeader(value = "Idempotency-Key", required = true)
            String idempotencyKey) {

        JiraTaskSyncResponse response =
                jiraIntegrationService.syncTask(
                        projectId,
                        taskId,
                        idempotencyKey);

        throwIfSyncFailed(response);

        return ResponseEntity.ok(
                ApiResponse.success(response));
    }

    /*
     * ============================================================
     * CNPM-80
     * Retry Task -> Jira
     * ============================================================
     */

    @PostMapping("/tasks/{taskId}/retry")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('TEAM_LEADER') and @projectAuthorization.isCurrentUserLeader(#projectId))")
    public ResponseEntity<?> retryTaskSync(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestHeader(value = "Idempotency-Key", required = true)
            String idempotencyKey) {

        JiraTaskSyncResponse response =
                jiraIntegrationService.retryTaskSync(
                        projectId,
                        taskId,
                        idempotencyKey);

        throwIfSyncFailed(response);

        return ResponseEntity.ok(
                ApiResponse.success(response));
    }


    private void throwIfSyncFailed(JiraTaskSyncResponse response) {
        if (response == null || response.errorCode() == null
                || response.errorCode().isBlank()) {
            return;
        }

        String code = response.errorCode();
        HttpStatus status;
        boolean retryable = response.retryable();

        if ("SYNC_ALREADY_RUNNING".equals(code)
                || "IDEMPOTENCY_KEY_REUSED".equals(code)
                || "DUPLICATE_REMOTE_ISSUE".equals(code)) {
            status = HttpStatus.CONFLICT;
        } else if ("JIRA_AUTHENTICATION_FAILED".equals(code)) {
            status = HttpStatus.UNAUTHORIZED;
        } else if ("JIRA_AUTHORIZATION_FAILED".equals(code)) {
            status = HttpStatus.FORBIDDEN;
        } else if ("JIRA_RESOURCE_NOT_FOUND".equals(code)) {
            status = HttpStatus.NOT_FOUND;
        } else if ("JIRA_CONNECTION_FAILED".equals(code)
                || "JIRA_SYNC_FAILED".equals(code)
                || code.endsWith("Exception")) {
            status = HttpStatus.BAD_GATEWAY;
        } else {
            status = HttpStatus.BAD_REQUEST;
        }

        throw new JiraApiException(
                status,
                code,
                retryable,
                null,
                response.message(),
                null);
    }
}

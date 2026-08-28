package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionRequest;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionTestResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;

@RestController
@RequestMapping("/api/v1/projects")
@PreAuthorize("hasRole('ADMIN')")
public class JiraConnectionController {

    private final JiraIntegrationService jiraIntegrationService;

    public JiraConnectionController(
            JiraIntegrationService jiraIntegrationService) {

        this.jiraIntegrationService =
                jiraIntegrationService;
    }

    @PostMapping("/{projectId}/integrations/jira/test-connection")
    public ResponseEntity<ApiResponse<JiraConnectionTestResponse>> testConnection(
            @PathVariable Long projectId,
            @RequestBody JiraConnectionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        jiraIntegrationService.testConnection(
                                projectId,
                                request.projectKey())));
    }
}
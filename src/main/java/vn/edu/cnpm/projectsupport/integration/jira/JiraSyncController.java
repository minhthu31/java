package vn.edu.cnpm.projectsupport.integration.jira;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.integration.jira.service.JiraSyncResult;
import vn.edu.cnpm.projectsupport.integration.jira.service.JiraSyncService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/jira")
public class JiraSyncController {

    private final JiraSyncService jiraSyncService;

    public JiraSyncController(JiraSyncService jiraSyncService) {
        this.jiraSyncService = jiraSyncService;
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('TEAM_LEADER')")
    public ApiResponse<JiraSyncResult> sync(
            @PathVariable Long projectId,
            @RequestParam String projectKey) {
        return ApiResponse.success(jiraSyncService.syncProject(projectId, projectKey));
    }
}

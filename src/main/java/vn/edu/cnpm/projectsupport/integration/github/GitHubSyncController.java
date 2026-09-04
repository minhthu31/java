package vn.edu.cnpm.projectsupport.integration.github;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.ApiResponse;
import vn.edu.cnpm.projectsupport.integration.github.service.GitHubRepositorySyncResult;
import vn.edu.cnpm.projectsupport.integration.github.service.GitHubRepositorySyncService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github")
public class GitHubSyncController {
    private final GitHubRepositorySyncService repositorySyncService;
    public GitHubSyncController(GitHubRepositorySyncService repositorySyncService) {
        this.repositorySyncService = repositorySyncService;
    }

    @PostMapping("/sync/repository")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.isCurrentUserLeader(#projectId)")
    public ApiResponse<GitHubRepositorySyncResult> syncRepository(@PathVariable Long projectId) {
        return ApiResponse.success(repositorySyncService.syncRepository(projectId));
    }
}

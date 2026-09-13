package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.security.ProjectAuthorizationService;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github/check-runs")
public class GitHubCheckRunController {

    private final GitHubCheckRunSyncService syncService;
    private final ProjectAuthorizationService projectAuthorization;

    public GitHubCheckRunController(
            GitHubCheckRunSyncService syncService,
            ProjectAuthorizationService projectAuthorization) {
        this.syncService = syncService;
        this.projectAuthorization = projectAuthorization;
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.isCurrentUserLeader(#projectId)")
    public ResponseEntity<Map<String, Object>> sync(@PathVariable Long projectId) {
        GitHubCheckRunSyncResult result = syncService.syncCheckRuns(projectId);
        return ResponseEntity.ok(wrap(result));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> list(
            @PathVariable Long projectId,
            @org.springframework.web.bind.annotation.RequestParam Long repositoryId) {
        return ResponseEntity.ok(wrap(syncService.listCheckRuns(projectId, repositoryId)));
    }

    private Map<String, Object> wrap(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}

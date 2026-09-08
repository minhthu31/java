package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github")
public class GitHubSyncController {

    private final GitHubCommitSyncService commitSyncService;
    private final GitHubPullRequestSyncService pullRequestSyncService;

    public GitHubSyncController(
            GitHubCommitSyncService commitSyncService,
            GitHubPullRequestSyncService pullRequestSyncService) {
        this.commitSyncService = commitSyncService;
        this.pullRequestSyncService = pullRequestSyncService;
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> sync(@PathVariable Long projectId) {
        GitHubCommitSyncResult commits = commitSyncService.syncCommits(projectId);
        GitHubPullRequestSyncResult pullRequests = pullRequestSyncService.syncPullRequests(projectId);
        GitHubSyncResult result = GitHubSyncResult.combine(projectId, commits, pullRequests);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", result);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}

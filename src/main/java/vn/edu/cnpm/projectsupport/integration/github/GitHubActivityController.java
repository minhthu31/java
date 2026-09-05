package vn.edu.cnpm.projectsupport.integration.github;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github")
@Validated
public class GitHubActivityController {

    private final GitHubActivityService gitHubActivityService;

    public GitHubActivityController(GitHubActivityService gitHubActivityService) {
        this.gitHubActivityService = gitHubActivityService;
    }

    @GetMapping("/repositories/{repositoryId}/commits")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> listCommits(
            @PathVariable Long projectId,
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @PageableDefault(size = 20, sort = "committedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<CommitResponse> data = gitHubActivityService.listCommits(projectId, repositoryId, issueKey, pageable);
        return wrapResponse(data);
    }

    @GetMapping("/repositories/{repositoryId}/pull-requests")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> listPullRequests(
            @PathVariable Long projectId,
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PullRequestResponse> data = gitHubActivityService.listPullRequests(projectId, repositoryId, state, issueKey, pageable);
        return wrapResponse(data);
    }

    @GetMapping("/activities")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> listActivities(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<GitHubActivityResponse> data = gitHubActivityService.listActivities(projectId, actorUserId, type, issueKey, from, to, pageable);
        return wrapResponse(data);
    }

    @GetMapping("/tasks/{taskId}/activities")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.canViewTask(#projectId, #taskId)")
    public ResponseEntity<Map<String, Object>> listTaskActivities(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam(required = false, defaultValue = "20") @Min(1) @Max(100) Integer size,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<GitHubActivityResponse> data = gitHubActivityService.listTaskActivities(projectId, taskId, pageable);
        return wrapResponse(data);
    }

    private ResponseEntity<Map<String, Object>> wrapResponse(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
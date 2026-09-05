package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github")
public class GitHubActivityController {

    private final GitHubActivityService activityService;

    public GitHubActivityController(GitHubActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping("/repositories/{repositoryId}/commits")
    @PreAuthorize("@projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> listCommits(
            @PathVariable Long projectId,
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String issueKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePageSize(size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(wrap(activityService.listCommits(projectId, repositoryId, issueKey, pageable)));
    }

    @GetMapping("/repositories/{repositoryId}/pull-requests")
    @PreAuthorize("@projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> listPullRequests(
            @PathVariable Long projectId,
            @PathVariable Long repositoryId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String issueKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePageSize(size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(wrap(activityService.listPullRequests(projectId, repositoryId, state, issueKey, pageable)));
    }

    @GetMapping("/activities")
    @PreAuthorize("@projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> listActivities(
            @PathVariable Long projectId,
            @RequestParam(required = false) Long actorUserId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String issueKey,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePageSize(size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(wrap(activityService.listActivities(projectId, actorUserId, type, issueKey, from, to, pageable)));
    }

    @GetMapping("/tasks/{taskId}/activities")
    @PreAuthorize("@projectAuthorization.canViewTask(#projectId, #taskId)")
    public ResponseEntity<Map<String, Object>> listTaskActivities(
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePageSize(size);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(wrap(activityService.listTaskActivities(projectId, taskId, pageable)));
    }

    private void validatePageSize(int size) {
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size phải từ 1 đến 100");
        }
    }

    private Map<String, Object> wrap(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
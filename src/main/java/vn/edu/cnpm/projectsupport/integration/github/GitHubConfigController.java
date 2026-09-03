package vn.edu.cnpm.projectsupport.integration.github;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
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

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github")
public class GitHubConfigController {

    private final GitHubConfigService gitHubConfigService;

    public GitHubConfigController(GitHubConfigService gitHubConfigService) {
        this.gitHubConfigService = gitHubConfigService;
    }

    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.isCurrentUserLeader(#projectId)")
    public ResponseEntity<Map<String, Object>> getConfig(@PathVariable Long projectId) {
        GitHubConfigResponse response = gitHubConfigService.getConfig(projectId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", response);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> configureGitHub(
            @PathVariable Long projectId,
            @Valid @RequestBody GitHubConfigRequest request) {
        GitHubConfigResponse response = gitHubConfigService.saveConfig(projectId, request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", response);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/test-connection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testConnection(@PathVariable Long projectId) {
        GitHubConnectionTestResponse response = gitHubConfigService.testConnection(projectId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", response);
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
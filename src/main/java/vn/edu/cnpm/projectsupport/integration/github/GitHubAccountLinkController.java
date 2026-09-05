package vn.edu.cnpm.projectsupport.integration.github;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github/members")
public class GitHubAccountLinkController {

    private final GitHubAccountLinkService gitHubAccountLinkService;

    public GitHubAccountLinkController(GitHubAccountLinkService gitHubAccountLinkService) {
        this.gitHubAccountLinkService = gitHubAccountLinkService;
    }

    @PutMapping("/{userId}/account-link")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.isCurrentUserLeader(#projectId)")
    public Map<String, Object> linkAccount(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            @Valid @RequestBody GitHubAccountLinkRequest request) {
        GitHubAccountLinkResponse response = gitHubAccountLinkService.linkAccount(projectId, userId, request);
        return envelope(response);
    }

    @GetMapping("/unlinked")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.isCurrentUserLeader(#projectId)")
    public Map<String, Object> listUnlinkedAccounts(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<GitHubUnlinkedAccountResponse> result = gitHubAccountLinkService.listUnlinkedAccounts(
                projectId, PageRequest.of(Math.max(page, 0), safeSize));
        return envelope(PageResponse.from(result));
    }

    private Map<String, Object> envelope(Object data) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", data);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}

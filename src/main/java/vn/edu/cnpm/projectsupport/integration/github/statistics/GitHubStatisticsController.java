package vn.edu.cnpm.projectsupport.integration.github.statistics;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations/github")
public class GitHubStatisticsController {

    private final GitHubStatisticsService statisticsService;

    public GitHubStatisticsController(GitHubStatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or @projectAuthorization.canViewTasks(#projectId)")
    public ResponseEntity<Map<String, Object>> getStatistics(
            @PathVariable Long projectId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", statisticsService.getMemberStatistics(projectId, from, to));
        body.put("timestamp", Instant.now().toString());
        body.put("from", from);
        body.put("to", to);
        return ResponseEntity.ok(body);
    }
}

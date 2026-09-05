package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.List;

public record GitHubActivityResponse(
        String type,
        String key,
        String summary,
        Long actorUserId,
        String actorLogin,
        Instant timestamp,
        String url,
        List<String> issueKeys,
        List<Long> linkedTaskIds
) {}
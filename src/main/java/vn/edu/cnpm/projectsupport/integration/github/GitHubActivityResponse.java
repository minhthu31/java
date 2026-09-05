package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;
import java.util.List;

public record GitHubActivityResponse(
        String type,
        String externalId,
        String title,
        Long actorUserId,
        String actorLogin,
        Instant occurredAt,
        String htmlUrl,
        List<String> issueKeys,
        List<Long> linkedTaskIds
) {}
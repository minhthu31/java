package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Instant;

/** Confirms which GitHub identity a member account is now linked to. */
public record GitHubAccountLinkResponse(
        Long userId,
        String externalAccountId,
        String username,
        String avatarUrl,
        String profileUrl,
        Instant linkedAt) {
}

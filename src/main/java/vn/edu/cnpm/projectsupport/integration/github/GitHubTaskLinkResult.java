package vn.edu.cnpm.projectsupport.integration.github;

import java.util.List;

/** Summary of one automatic Task-to-GitHub activity linking attempt. */
public record GitHubTaskLinkResult(
        int keysDetected,
        int linksCreated,
        int duplicateLinks,
        List<String> warnings) {

    public GitHubTaskLinkResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

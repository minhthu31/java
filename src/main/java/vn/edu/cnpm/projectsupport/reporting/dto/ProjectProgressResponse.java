package vn.edu.cnpm.projectsupport.reporting.dto;

import java.time.Instant;
import java.util.List;

public record ProjectProgressResponse(
        Long projectId,
        long totalRequirements,
        long totalFeatures,
        long totalSprints,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        double progressPercent,
        List<SprintProgressResponse> sprints,
        Instant asOf) {
}

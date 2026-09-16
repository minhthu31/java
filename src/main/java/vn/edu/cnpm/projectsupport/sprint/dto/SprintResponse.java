package vn.edu.cnpm.projectsupport.sprint.dto;

import java.time.Instant;

public record SprintResponse(
        Long id,
        Long projectId,
        Long jiraSprintId,
        String name,
        String state,
        String goal,
        Instant startDate,
        Instant endDate,
        Instant lastSyncedAt) {
}

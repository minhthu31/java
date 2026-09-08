package vn.edu.cnpm.projectsupport.reporting.dto;

import java.util.Map;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

/** Current task snapshot calculated at the report {@code asOf} instant. */
public record TaskMetricsResponse(
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        Map<TaskStatus, Long> tasksByStatus) {
}

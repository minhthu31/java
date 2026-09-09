package vn.edu.cnpm.projectsupport.reporting.dto;

public record SprintProgressResponse(
        Long sprintId,
        String sprintName,
        long totalTasks,
        long completedTasks,
        long overdueTasks,
        double progressPercent) {
}

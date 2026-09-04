package vn.edu.cnpm.projectsupport.task.repository;

import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.dto.TaskFilterRequest;

public final class TaskSpecification {

    private TaskSpecification() {
    }

    public static Specification<Task> matches(
            Long projectId,
            TaskFilterRequest filter,
            Long forcedAssigneeId) {
        return (root, query, builder) -> {
            var predicate = builder.conjunction();
            predicate = builder.and(predicate, builder.equal(root.get("projectId"), projectId));

            if (filter.getStatus() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPriority() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getIssueType() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("issueType"), filter.getIssueType()));
            }
            if (filter.getClassification() != null) {
                predicate = builder.and(
                        predicate,
                        builder.equal(root.get("classification"), filter.getClassification()));
            }
            Long assigneeId = forcedAssigneeId == null ? filter.getAssigneeId() : forcedAssigneeId;
            if (assigneeId != null) {
                predicate = builder.and(predicate, builder.equal(root.get("assigneeUserId"), assigneeId));
            }
            if (filter.getRequirementId() != null) {
                predicate = builder.and(
                        predicate,
                        builder.equal(root.get("requirementId"), filter.getRequirementId()));
            }
            if (filter.getFeatureId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("featureId"), filter.getFeatureId()));
            }
            if (filter.getSprintId() != null) {
                predicate = builder.and(predicate, builder.equal(root.get("sprintId"), filter.getSprintId()));
            }
            if (filter.getSyncStatus() != null) {
                predicate = builder.and(
                        predicate,
                        builder.equal(root.get("syncStatus"), filter.getSyncStatus()));
            }
            if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                String keyword = "%" + filter.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("title")), keyword),
                        builder.like(builder.lower(root.get("description")), keyword),
                        builder.like(builder.lower(root.get("acceptanceCriteria")), keyword)));
            }
            return predicate;
        };
    }
}

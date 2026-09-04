package vn.edu.cnpm.projectsupport.requirement;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

final class RequirementSpecification {

    private RequirementSpecification() {
    }

    static Specification<Requirement> matches(Long projectId, RequirementFilterRequest filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("projectId"), projectId));

            if (filter.getStatus() != null) {
                predicates.add(builder.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getPriority() != null) {
                predicates.add(builder.equal(root.get("priority"), filter.getPriority()));
            }
            if (StringUtils.hasText(filter.getJiraIssueKey())) {
                predicates.add(builder.equal(root.get("jiraIssueKey"), filter.getJiraIssueKey().trim()));
            }
            if (StringUtils.hasText(filter.getKeyword())) {
                String pattern = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern),
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("actor")), pattern)));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

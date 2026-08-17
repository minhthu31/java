package vn.edu.cnpm.projectsupport.requirement;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RequirementSpecification {

    public static Specification<Requirement> filterRequirements(RequirementFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            
            predicates.add(cb.equal(root.get("isDeleted"), false));

            
            if (Objects.nonNull(filter.getProjectId()) && StringUtils.hasText(String.valueOf(filter.getProjectId()))) {
                predicates.add(cb.equal(root.get("projectId"), String.valueOf(filter.getProjectId())));
            }

            if (StringUtils.hasText(filter.getKeyword())) {
                String searchPattern = "%" + filter.getKeyword().toLowerCase() + "%";
                Predicate titleLike = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleLike, descLike));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
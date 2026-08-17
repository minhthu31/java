package com.example.requirement;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class RequirementSpecification {

    public static Specification<Requirement> filterRequirements(RequirementFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Lọc dữ liệu chưa bị xóa mềm
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (StringUtils.hasText(filter.getProjectId())) {
                predicates.add(cb.equal(root.get("projectId"), filter.getProjectId()));
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

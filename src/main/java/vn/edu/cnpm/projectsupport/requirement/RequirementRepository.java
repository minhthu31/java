package vn.edu.cnpm.projectsupport.requirement;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequirementRepository
        extends JpaRepository<Requirement, Long>, JpaSpecificationExecutor<Requirement> {

    Optional<Requirement> findByIdAndProjectId(Long id, Long projectId);
}

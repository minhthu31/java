package vn.edu.cnpm.projectsupport.sprint.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.sprint.domain.Sprint;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findByProjectId(Long projectId);
    Optional<Sprint> findByProjectIdAndJiraSprintId(Long projectId, Long jiraSprintId);
    Optional<Sprint> findByIdAndProjectId(Long id, Long projectId);
}

package vn.edu.cnpm.projectsupport.integration.jira.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraBacklogSnapshot;

public interface JiraBacklogSnapshotRepository extends JpaRepository<JiraBacklogSnapshot, Long> {
    Optional<JiraBacklogSnapshot> findByProjectId(Long projectId);
}

package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskCommitLinkId;

public interface TaskCommitLinkRepository extends JpaRepository<TaskCommitLink, TaskCommitLinkId> {
    List<TaskCommitLink> findByIdTaskId(Long taskId);
    List<TaskCommitLink> findByIdCommitId(Long commitId);
}

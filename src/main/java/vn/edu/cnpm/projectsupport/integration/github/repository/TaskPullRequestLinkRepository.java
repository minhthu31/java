package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLink;
import vn.edu.cnpm.projectsupport.integration.github.domain.TaskPullRequestLinkId;

public interface TaskPullRequestLinkRepository extends JpaRepository<TaskPullRequestLink, TaskPullRequestLinkId> {
    List<TaskPullRequestLink> findByIdTaskId(Long taskId);
    List<TaskPullRequestLink> findByIdPullRequestId(Long pullRequestId);
}

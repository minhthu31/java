package vn.edu.cnpm.projectsupport.task.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssigneeUserId(Long assigneeUserId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByIssueType(TaskIssueType issueType);
    Optional<Task> findByIdempotencyKey(String idempotencyKey);
}

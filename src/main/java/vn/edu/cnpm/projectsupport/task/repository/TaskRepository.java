package vn.edu.cnpm.projectsupport.task.repository;

import java.util.List;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByProject_Id(Long projectId);

    List<Task> findByAssignee_Id(Long assigneeId);

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByIssueType(TaskIssueType issueType);

    List<Task> findByProject_IdAndAssignee_Id(Long projectId,Long assigneeId);

    List<Task> findByProject_IdAndStatus(Long projectId,TaskStatus status);

    List<Task> findByProject_IdAndIssueType(Long projectId,TaskIssueType issueType);
}
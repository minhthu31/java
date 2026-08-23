package vn.edu.cnpm.projectsupport.task.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.domain.TaskIssueType;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByProjectIdAndAssigneeUserId(Long projectId, Long assigneeUserId);
    List<Task> findByAssigneeUserId(Long assigneeUserId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByIssueType(TaskIssueType issueType);
    Optional<Task> findByIdempotencyKey(String idempotencyKey);
    boolean existsByRequirementId(Long requirementId);

    @Query(value = "SELECT jira_issue_key FROM jira_issues WHERE task_id = :taskId", nativeQuery = true)
    Optional<String> findJiraIssueKeyByTaskId(@Param("taskId") Long taskId);

    @Query(value = "SELECT COUNT(*) FROM jira_issues WHERE task_id = :taskId", nativeQuery = true)
    long countJiraIssuesByTaskId(@Param("taskId") Long taskId);

    @Query(value = "SELECT COUNT(*) FROM task_commit_links WHERE task_id = :taskId", nativeQuery = true)
    long countCommitLinksByTaskId(@Param("taskId") Long taskId);

    @Query(value = "SELECT COUNT(*) FROM task_pr_links WHERE task_id = :taskId", nativeQuery = true)
    long countPullRequestLinksByTaskId(@Param("taskId") Long taskId);
}

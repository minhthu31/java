package vn.edu.cnpm.projectsupport.reporting.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.task.domain.TaskStatus;

/** Read-only aggregate queries used by the project report. */
public interface ReportingRepository extends JpaRepository<vn.edu.cnpm.projectsupport.task.domain.Task, Long> {

    interface StatusCountProjection {
        String getStatus();
        long getCount();
    }

    @Query(value = """
            SELECT t.status AS status, COUNT(DISTINCT t.id) AS count
              FROM tasks t
             WHERE t.project_id = :projectId
               AND (:sprintId IS NULL OR t.sprint_id = :sprintId)
               AND (:memberId IS NULL OR t.assignee_user_id = :memberId)
               AND (:fromTime IS NULL OR t.created_at >= :fromTime)
               AND (:toTime IS NULL OR t.created_at < :toTime)
               AND t.created_at <= :asOf
             GROUP BY t.status
            """, nativeQuery = true)
    List<StatusCountProjection> countTasksByStatus(
            @Param("projectId") Long projectId,
            @Param("sprintId") Long sprintId,
            @Param("memberId") Long memberId,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("asOf") Instant asOf);

    @Query(value = """
            SELECT COUNT(DISTINCT c.id)
              FROM github_commits c
              JOIN github_repositories r ON r.id = c.repository_id
             WHERE r.project_id = :projectId
               AND c.author_external_account_id = (
                   SELECT a.id
                     FROM user_external_accounts a
                    WHERE a.user_id = :memberId
                      AND a.provider = 'GITHUB'
               )
               AND (:fromTime IS NULL OR c.committed_at >= :fromTime)
               AND (:toTime IS NULL OR c.committed_at < :toTime)
               AND c.created_at <= :asOf
               AND (
                   :sprintId IS NULL OR EXISTS (
                       SELECT 1
                         FROM task_commit_links l
                         JOIN tasks t ON t.id = l.task_id
                        WHERE l.commit_id = c.id
                          AND t.project_id = :projectId
                          AND t.sprint_id = :sprintId
                   )
               )
            """, nativeQuery = true)
    long countCommits(
            @Param("projectId") Long projectId,
            @Param("sprintId") Long sprintId,
            @Param("memberId") Long memberId,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("asOf") Instant asOf);

    @Query(value = """
            SELECT COUNT(DISTINCT pr.id)
              FROM github_pull_requests pr
              JOIN github_repositories r ON r.id = pr.repository_id
             WHERE r.project_id = :projectId
               AND pr.author_external_account_id = (
                   SELECT a.id
                     FROM user_external_accounts a
                    WHERE a.user_id = :memberId
                      AND a.provider = 'GITHUB'
               )
               AND (:fromTime IS NULL OR pr.remote_created_at >= :fromTime)
               AND (:toTime IS NULL OR pr.remote_created_at < :toTime)
               AND pr.remote_created_at <= :asOf
               AND (
                   :sprintId IS NULL OR EXISTS (
                       SELECT 1
                         FROM task_pr_links l
                         JOIN tasks t ON t.id = l.task_id
                        WHERE l.pull_request_id = pr.id
                          AND t.project_id = :projectId
                          AND t.sprint_id = :sprintId
                   )
               )
            """, nativeQuery = true)
    long countPullRequests(
            @Param("projectId") Long projectId,
            @Param("sprintId") Long sprintId,
            @Param("memberId") Long memberId,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("asOf") Instant asOf);

    @Query(value = """
            SELECT COUNT(DISTINCT pr.id)
              FROM github_pull_requests pr
              JOIN github_repositories r ON r.id = pr.repository_id
             WHERE r.project_id = :projectId
               AND pr.author_external_account_id = (
                   SELECT a.id
                     FROM user_external_accounts a
                    WHERE a.user_id = :memberId
                      AND a.provider = 'GITHUB'
               )
               AND pr.state = :state
               AND (:fromTime IS NULL OR pr.remote_created_at >= :fromTime)
               AND (:toTime IS NULL OR pr.remote_created_at < :toTime)
               AND pr.remote_created_at <= :asOf
               AND (
                   :sprintId IS NULL OR EXISTS (
                       SELECT 1
                         FROM task_pr_links l
                         JOIN tasks t ON t.id = l.task_id
                        WHERE l.pull_request_id = pr.id
                          AND t.project_id = :projectId
                          AND t.sprint_id = :sprintId
                   )
               )
            """, nativeQuery = true)
    long countPullRequestsByState(
            @Param("projectId") Long projectId,
            @Param("sprintId") Long sprintId,
            @Param("memberId") Long memberId,
            @Param("state") String state,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("asOf") Instant asOf);

    @Query(value = """
            SELECT COUNT(DISTINCT x.task_id)
              FROM (
                    SELECT l.task_id
                      FROM task_commit_links l
                      JOIN github_commits c ON c.id = l.commit_id
                      JOIN github_repositories r ON r.id = c.repository_id
                      JOIN user_external_accounts a ON a.id = c.author_external_account_id
                     WHERE r.project_id = :projectId
                       AND a.user_id = :memberId
                       AND a.provider = 'GITHUB'
                       AND (:fromTime IS NULL OR c.committed_at >= :fromTime)
                       AND (:toTime IS NULL OR c.committed_at < :toTime)
                       AND c.created_at <= :asOf
                    UNION
                    SELECT l.task_id
                      FROM task_pr_links l
                      JOIN github_pull_requests pr ON pr.id = l.pull_request_id
                      JOIN github_repositories r ON r.id = pr.repository_id
                      JOIN user_external_accounts a ON a.id = pr.author_external_account_id
                     WHERE r.project_id = :projectId
                       AND a.user_id = :memberId
                       AND a.provider = 'GITHUB'
                       AND (:fromTime IS NULL OR pr.remote_created_at >= :fromTime)
                       AND (:toTime IS NULL OR pr.remote_created_at < :toTime)
                       AND pr.remote_created_at <= :asOf
                       AND pr.created_at <= :asOf
                   ) x
              JOIN tasks t ON t.id = x.task_id
             WHERE t.project_id = :projectId
               AND (:sprintId IS NULL OR t.sprint_id = :sprintId)
            """, nativeQuery = true)
    long countLinkedTasks(
            @Param("projectId") Long projectId,
            @Param("sprintId") Long sprintId,
            @Param("memberId") Long memberId,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("asOf") Instant asOf);

    @Query(value = """
            SELECT CASE WHEN COUNT(a.id) > 0 THEN TRUE ELSE FALSE END
              FROM user_external_accounts a
             WHERE a.user_id = :memberId
               AND a.provider = 'GITHUB'
            """, nativeQuery = true)
    boolean isGithubLinked(@Param("memberId") Long memberId);

    @Query(value = """
            SELECT MAX(s.completed_at)
              FROM sync_logs s
             WHERE s.project_id = :projectId
               AND s.provider = 'GITHUB'
               AND s.status = 'SUCCESS'
            """, nativeQuery = true)
    Instant findLastSuccessfulGithubSync(@Param("projectId") Long projectId);

    @Query(value = """
            SELECT COUNT(DISTINCT t.id)
              FROM tasks t
             WHERE t.project_id = :projectId
               AND (:sprintId IS NULL OR t.sprint_id = :sprintId)
               AND (:memberId IS NULL OR t.assignee_user_id = :memberId)
               AND (:fromTime IS NULL OR t.created_at >= :fromTime)
               AND (:toTime IS NULL OR t.created_at < :toTime)
               AND t.created_at <= :asOf
               AND t.deadline IS NOT NULL
               AND t.deadline < :asOf
               AND t.status NOT IN ('DONE', 'CANCELLED')
            """, nativeQuery = true)
    long countOverdueTasks(
            @Param("projectId") Long projectId,
            @Param("sprintId") Long sprintId,
            @Param("memberId") Long memberId,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("asOf") Instant asOf);

}

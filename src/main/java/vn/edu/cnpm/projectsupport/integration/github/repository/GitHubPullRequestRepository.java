package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequestState;

public interface GitHubPullRequestRepository extends JpaRepository<GitHubPullRequest, Long> {

    Optional<GitHubPullRequest> findByRepositoryIdAndNumber(Long repositoryId, Integer number);

    Page<GitHubPullRequest> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId, Pageable pageable);

    Page<GitHubPullRequest> findByRepositoryIdAndStateOrderByCreatedAtDesc(
            Long repositoryId, GitHubPullRequestState state, Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            where pr.repositoryId = :repositoryId
              and (:state is null or pr.state = :state)
            order by pr.remoteCreatedAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findByRepositoryIdAndState(
            @Param("repositoryId") Long repositoryId,
            @Param("state") GitHubPullRequestState state,
            Pageable pageable);

    @Query("""
            select distinct pr from GitHubPullRequest pr
            join TaskPullRequestLink tpl on tpl.id.pullRequestId = pr.id
            join JiraIssue ji on ji.taskId = tpl.id.taskId
            where pr.repositoryId = :repositoryId
              and (:state is null or pr.state = :state)
              and ji.jiraIssueKey = :issueKey
            order by pr.remoteCreatedAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findByRepositoryIdAndStateAndExactIssueKey(
            @Param("repositoryId") Long repositoryId,
            @Param("state") GitHubPullRequestState state,
            @Param("issueKey") String issueKey,
            Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            left join UserExternalAccount a on a.id = pr.authorExternalAccountId
            where r.projectId = :projectId
              and (:userId is null or a.userId = :userId)
              and (:state is null or pr.state = :state)
              and (:from is null or pr.remoteCreatedAt >= :from)
              and (:to is null or pr.remoteCreatedAt <= :to)
            order by pr.remoteCreatedAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findUnifiedActivityWithoutIssueKey(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("state") GitHubPullRequestState state,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            select distinct pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            left join UserExternalAccount a on a.id = pr.authorExternalAccountId
            join TaskPullRequestLink tpl on tpl.id.pullRequestId = pr.id
            join JiraIssue ji on ji.taskId = tpl.id.taskId
            where r.projectId = :projectId
              and (:userId is null or a.userId = :userId)
              and (:state is null or pr.state = :state)
              and ji.jiraIssueKey = :issueKey
              and (:from is null or pr.remoteCreatedAt >= :from)
              and (:to is null or pr.remoteCreatedAt <= :to)
            order by pr.remoteCreatedAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findUnifiedActivityWithIssueKey(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("state") GitHubPullRequestState state,
            @Param("issueKey") String issueKey,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            join TaskPullRequestLink l on l.id.pullRequestId = pr.id
            where l.id.taskId = :taskId
            order by pr.remoteCreatedAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findByTaskIdPaged(@Param("taskId") Long taskId, Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            where r.projectId = :projectId
            order by pr.remoteCreatedAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findActivityByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            join UserExternalAccount a on a.id = pr.authorExternalAccountId
            where r.projectId = :projectId and a.userId = :userId
            order by pr.remoteCreatedAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findActivityByProjectIdAndUserId(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
            update GitHubPullRequest pr
               set pr.authorExternalAccountId = :accountId
             where pr.authorExternalAccountId is null
               and pr.authorGithubUserId = :githubUserId
               and pr.repositoryId in (
                   select r.id from GitHubRepository r where r.projectId = :projectId
               )
            """)
    int backfillAuthorExternalAccountId(
            @Param("projectId") Long projectId,
            @Param("githubUserId") Long githubUserId,
            @Param("accountId") Long accountId);

    @Query("""
            select distinct pr.authorGithubUserId as githubUserId, pr.authorLogin as login
            from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            where r.projectId = :projectId
              and pr.authorGithubUserId is not null
              and pr.authorExternalAccountId is null
            """)
    List<GitHubUnlinkedAuthorProjection> findUnlinkedAuthors(@Param("projectId") Long projectId);
    @Query(value = """
            SELECT pr.id AS activityId,
                   a.user_id AS userId,
                   tpl.task_id AS taskId
              FROM github_pull_requests pr
              JOIN github_repositories r ON r.id = pr.repository_id
              LEFT JOIN user_external_accounts a ON a.id = pr.author_external_account_id
              LEFT JOIN task_pr_links tpl ON tpl.pull_request_id = pr.id
             WHERE r.project_id = :projectId
               AND (:memberId IS NULL OR a.user_id = :memberId)
               AND (:from IS NULL OR pr.remote_created_at >= :from)
               AND (:to IS NULL OR pr.remote_created_at < :to)
               AND (:sprintId IS NULL OR EXISTS (
                    SELECT 1 FROM tasks t
                     WHERE t.id = tpl.task_id
                       AND t.project_id = :projectId
                       AND t.sprint_id = :sprintId
                       AND (:taskMemberId IS NULL OR t.assignee_user_id = :taskMemberId)
               ))
            """, nativeQuery = true)
    List<ReportPullRequestActivityProjection> findReportActivity(
            @Param("projectId") Long projectId,
            @Param("memberId") Long memberId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("sprintId") Long sprintId,
            @Param("taskMemberId") Long taskMemberId);

}

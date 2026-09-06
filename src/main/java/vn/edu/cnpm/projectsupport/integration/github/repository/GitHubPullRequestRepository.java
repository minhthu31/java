package vn.edu.cnpm.projectsupport.integration.github.repository;

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
            order by pr.createdAt desc, pr.id desc
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
            order by pr.createdAt desc, pr.id desc
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
              and (:from is null or pr.createdAt >= :from)
              and (:to is null or pr.createdAt <= :to)
            order by pr.createdAt desc, pr.id desc
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
              and (:from is null or pr.createdAt >= :from)
              and (:to is null or pr.createdAt <= :to)
            order by pr.createdAt desc, pr.id desc
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
            order by pr.createdAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findByTaskIdPaged(@Param("taskId") Long taskId, Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            where r.projectId = :projectId
            order by pr.createdAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findActivityByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            join UserExternalAccount a on a.id = pr.authorExternalAccountId
            where r.projectId = :projectId and a.userId = :userId
            order by pr.createdAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findActivityByProjectIdAndUserId(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            Pageable pageable);
}
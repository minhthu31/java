package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;

public interface GitHubCommitRepository extends JpaRepository<GitHubCommit, Long> {

    Optional<GitHubCommit> findByRepositoryIdAndSha(Long repositoryId, String sha);

    Page<GitHubCommit> findByRepositoryIdOrderByCommittedAtDesc(Long repositoryId, Pageable pageable);

    @Query("""
            select distinct c from GitHubCommit c
            join TaskCommitLink tcl on tcl.id.commitId = c.id
            join JiraIssue ji on ji.taskId = tcl.id.taskId
            where c.repositoryId = :repositoryId
              and ji.jiraIssueKey = :issueKey
            order by c.committedAt desc, c.id desc
            """)
    Page<GitHubCommit> findByRepositoryIdAndExactIssueKey(
            @Param("repositoryId") Long repositoryId,
            @Param("issueKey") String issueKey,
            Pageable pageable);

    @Query("""
            select c from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            left join UserExternalAccount a on a.id = c.authorExternalAccountId
            where r.projectId = :projectId
              and (:userId is null or a.userId = :userId)
              and (:from is null or c.committedAt >= :from)
              and (:to is null or c.committedAt <= :to)
            order by c.committedAt desc, c.id desc
            """)
    Page<GitHubCommit> findUnifiedActivityWithoutIssueKey(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            select distinct c from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            left join UserExternalAccount a on a.id = c.authorExternalAccountId
            join TaskCommitLink tcl on tcl.id.commitId = c.id
            join JiraIssue ji on ji.taskId = tcl.id.taskId
            where r.projectId = :projectId
              and (:userId is null or a.userId = :userId)
              and ji.jiraIssueKey = :issueKey
              and (:from is null or c.committedAt >= :from)
              and (:to is null or c.committedAt <= :to)
            order by c.committedAt desc, c.id desc
            """)
    Page<GitHubCommit> findUnifiedActivityWithIssueKey(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            @Param("issueKey") String issueKey,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);

    @Query("""
            select c from GitHubCommit c
            join TaskCommitLink l on l.id.commitId = c.id
            where l.id.taskId = :taskId
            order by c.committedAt desc, c.id desc
            """)
    Page<GitHubCommit> findByTaskIdPaged(@Param("taskId") Long taskId, Pageable pageable);

    @Query("""
            select c from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            where r.projectId = :projectId
            order by c.committedAt desc, c.id desc
            """)
    Page<GitHubCommit> findActivityByProjectId(@Param("projectId") Long projectId, Pageable pageable);

    @Query("""
            select c from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            join UserExternalAccount a on a.id = c.authorExternalAccountId
            where r.projectId = :projectId and a.userId = :userId
            order by c.committedAt desc, c.id desc
            """)
    Page<GitHubCommit> findActivityByProjectIdAndUserId(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("""
            update GitHubCommit c
               set c.authorExternalAccountId = :accountId
             where c.authorExternalAccountId is null
               and c.authorGithubUserId = :githubUserId
               and c.repositoryId in (
                   select r.id from GitHubRepository r where r.projectId = :projectId
               )
            """)
    int backfillAuthorExternalAccountId(
            @Param("projectId") Long projectId,
            @Param("githubUserId") Long githubUserId,
            @Param("accountId") Long accountId);

    @Query("""
            select a.userId as userId, count(c.id) as count
            from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            join UserExternalAccount a on a.id = c.authorExternalAccountId
            where r.projectId = :projectId
              and a.provider = vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider.GITHUB
              and (:from is null or c.committedAt >= :from)
              and (:to is null or c.committedAt <= :to)
            group by a.userId
            """)
    List<GitHubUserActivityCountProjection> countByProjectIdAndTimeRange(
            @Param("projectId") Long projectId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Query("""
            select distinct c.authorGithubUserId as githubUserId, c.authorLogin as login
            from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            where r.projectId = :projectId
              and c.authorGithubUserId is not null
              and c.authorExternalAccountId is null
            """)
    List<GitHubUnlinkedAuthorProjection> findUnlinkedAuthors(@Param("projectId") Long projectId);
}

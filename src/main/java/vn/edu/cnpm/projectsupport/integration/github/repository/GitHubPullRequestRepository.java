package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.time.Instant;
import java.util.List;
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
              and (:issueKey is null or (pr.headRef like concat('%', :issueKey, '%')
                                     or pr.title like concat('%', :issueKey, '%')
                                     or pr.body like concat('%', :issueKey, '%')))
            order by pr.createdAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findByRepositoryIdAndFilter(
            @Param("repositoryId") Long repositoryId,
            @Param("state") GitHubPullRequestState state,
            @Param("issueKey") String issueKey,
            Pageable pageable);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            where r.projectId = :projectId
              and (:actorUserId is null or pr.authorExternalAccountId = :actorUserId)
              and (:state is null or pr.state = :state)
              and (:issueKey is null or (pr.headRef like concat('%', :issueKey, '%')
                                     or pr.title like concat('%', :issueKey, '%')
                                     or pr.body like concat('%', :issueKey, '%')))
              and (:from is null or pr.createdAt >= :from)
              and (:to is null or pr.createdAt <= :to)
            order by pr.createdAt desc, pr.id desc
            """)
    Page<GitHubPullRequest> findUnifiedActivity(
            @Param("projectId") Long projectId,
            @Param("actorUserId") Long actorUserId,
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
    List<GitHubPullRequest> findByTaskId(@Param("taskId") Long taskId);

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
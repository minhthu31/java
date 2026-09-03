package vn.edu.cnpm.projectsupport.integration.github.repository;

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

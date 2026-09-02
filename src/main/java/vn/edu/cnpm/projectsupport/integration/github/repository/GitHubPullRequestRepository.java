package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubPullRequest;

public interface GitHubPullRequestRepository extends JpaRepository<GitHubPullRequest, Long> {

    Optional<GitHubPullRequest> findByRepositoryIdAndNumber(Long repositoryId, Integer number);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            where r.projectId = :projectId
            order by pr.createdAt desc
            """)
    List<GitHubPullRequest> findActivityByProjectId(@Param("projectId") Long projectId);

    @Query("""
            select pr from GitHubPullRequest pr
            join GitHubRepository r on r.id = pr.repositoryId
            join UserExternalAccount a on a.id = pr.authorExternalAccountId
            where r.projectId = :projectId and a.userId = :userId
            order by pr.createdAt desc
            """)
    List<GitHubPullRequest> findActivityByProjectIdAndUserId(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);
}

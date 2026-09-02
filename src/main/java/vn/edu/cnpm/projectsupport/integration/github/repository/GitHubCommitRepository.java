package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCommit;

public interface GitHubCommitRepository extends JpaRepository<GitHubCommit, Long> {

    Optional<GitHubCommit> findByRepositoryIdAndSha(Long repositoryId, String sha);

    @Query("""
            select c from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            where r.projectId = :projectId
            order by c.committedAt desc
            """)
    List<GitHubCommit> findActivityByProjectId(@Param("projectId") Long projectId);

    @Query("""
            select c from GitHubCommit c
            join GitHubRepository r on r.id = c.repositoryId
            join UserExternalAccount a on a.id = c.authorExternalAccountId
            where r.projectId = :projectId and a.userId = :userId
            order by c.committedAt desc
            """)
    List<GitHubCommit> findActivityByProjectIdAndUserId(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);
}

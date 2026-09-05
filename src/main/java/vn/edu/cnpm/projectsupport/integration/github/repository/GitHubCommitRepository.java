package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
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

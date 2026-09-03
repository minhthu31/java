package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;

public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, Long> {
    Page<GitHubRepository> findByProjectIdOrderByFullNameAsc(Long projectId, Pageable pageable);
    Optional<GitHubRepository> findByGithubRepositoryId(Long githubRepositoryId);
    Optional<GitHubRepository> findByProjectIdAndGithubRepositoryId(Long projectId, Long githubRepositoryId);
}

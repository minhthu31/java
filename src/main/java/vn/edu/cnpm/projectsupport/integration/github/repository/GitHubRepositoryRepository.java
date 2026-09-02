package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubRepository;

public interface GitHubRepositoryRepository extends JpaRepository<GitHubRepository, Long> {
    List<GitHubRepository> findByProjectIdOrderByFullNameAsc(Long projectId);
    Optional<GitHubRepository> findByGithubRepositoryId(Long githubRepositoryId);
    Optional<GitHubRepository> findByProjectIdAndGithubRepositoryId(Long projectId, Long githubRepositoryId);
}

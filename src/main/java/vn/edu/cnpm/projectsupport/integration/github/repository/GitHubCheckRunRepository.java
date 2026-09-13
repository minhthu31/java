package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.GitHubCheckRun;

public interface GitHubCheckRunRepository extends JpaRepository<GitHubCheckRun, Long> {

    Optional<GitHubCheckRun> findByRepositoryIdAndExternalId(Long repositoryId, Long externalId);

    List<GitHubCheckRun> findByRepositoryIdOrderByCompletedAtDesc(Long repositoryId);
}

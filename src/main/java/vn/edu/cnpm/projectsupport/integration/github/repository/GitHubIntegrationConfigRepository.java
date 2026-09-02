package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;

public interface GitHubIntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {

    Optional<IntegrationConfig> findByProjectIdAndProvider(Long projectId, IntegrationProvider provider);

    default Optional<IntegrationConfig> findGitHubConfigByProjectId(Long projectId) {
        return findByProjectIdAndProvider(projectId, IntegrationProvider.GITHUB);
    }
}

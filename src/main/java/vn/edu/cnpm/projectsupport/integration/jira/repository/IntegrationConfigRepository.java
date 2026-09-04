package vn.edu.cnpm.projectsupport.integration.jira.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;

public interface IntegrationConfigRepository extends JpaRepository<IntegrationConfig, Long> {

    Optional<IntegrationConfig> findByProjectIdAndProvider(
            Long projectId,
            IntegrationProvider provider);

    boolean existsByProjectIdAndProvider(Long projectId, IntegrationProvider provider);
}

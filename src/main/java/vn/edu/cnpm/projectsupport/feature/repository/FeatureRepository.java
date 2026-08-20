package vn.edu.cnpm.projectsupport.feature.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.feature.domain.Feature;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    List<Feature> findByProjectId(Long projectId);
    Optional<Feature> findByProjectIdAndJiraEpicKey(Long projectId, String jiraEpicKey);
}

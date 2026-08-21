package vn.edu.cnpm.projectsupport.audit.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.audit.domain.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, String entityId);
}

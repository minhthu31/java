package vn.edu.cnpm.projectsupport.integration.jira.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    Page<SyncLog> findByProjectIdOrderByStartedAtDesc(Long projectId, Pageable pageable);

    Page<SyncLog> findByProjectIdAndStatusOrderByStartedAtDesc(
            Long projectId,
            SyncLogStatus status,
            Pageable pageable);

    List<SyncLog> findByProjectIdAndCorrelationIdOrderByStartedAtDesc(
            Long projectId,
            String correlationId);

    List<SyncLog> findByProjectIdAndStatusAndCorrelationIdOrderByStartedAtDesc(
            Long projectId,
            SyncLogStatus status,
            String correlationId);
}

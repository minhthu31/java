package vn.edu.cnpm.projectsupport.integration.jira.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    Optional<SyncLog> findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
            Long projectId,
            String entityType,
            String entityId,
            String idempotencyKey);

    Page<SyncLog> findByProjectIdOrderByStartedAtDesc(
            Long projectId,
            Pageable pageable);

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

package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraAuthType;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionRequest;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionTestResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraTaskSyncResponse;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfigStatus;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.domain.JiraIssue;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncDirection;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLog;
import vn.edu.cnpm.projectsupport.integration.jira.domain.SyncLogStatus;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueRequest;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.JiraIssueRepository;
import vn.edu.cnpm.projectsupport.integration.jira.repository.SyncLogRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.task.domain.SyncStatus;
import vn.edu.cnpm.projectsupport.task.domain.Task;
import vn.edu.cnpm.projectsupport.task.repository.TaskRepository;

@Service
@Transactional
public class JiraIntegrationServiceImpl implements JiraIntegrationService {

    private static final String ENTITY_TYPE_TASK = "TASK";
    private static final Map<Long, Object> TASK_LOCKS = new ConcurrentHashMap<>();

    private final IntegrationConfigRepository configRepository;
    private final IntegrationSecretService secretService;
    private final JiraClient jiraClient;
    private final JiraIssueRepository jiraIssueRepository;
    private final SyncLogRepository syncLogRepository;
    private final TaskRepository taskRepository;
    private final JdbcTemplate jdbcTemplate;

    public JiraIntegrationServiceImpl(
            IntegrationConfigRepository configRepository,
            IntegrationSecretService secretService,
            JiraClient jiraClient,
            JiraIssueRepository jiraIssueRepository,
            SyncLogRepository syncLogRepository,
            TaskRepository taskRepository,
            JdbcTemplate jdbcTemplate) {

        this.configRepository = configRepository;
        this.secretService = secretService;
        this.jiraClient = jiraClient;
        this.jiraIssueRepository = jiraIssueRepository;
        this.syncLogRepository = syncLogRepository;
        this.taskRepository = taskRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public JiraConnectionResponse getConnection(Long projectId) {

        IntegrationConfig config =
                configRepository
                        .findByProjectIdAndProvider(
                                projectId,
                                IntegrationProvider.JIRA)
                        .orElse(null);

        if (config == null) {
            return new JiraConnectionResponse(
                    projectId,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    null);
        }

        String projectKey =
                getSavedJiraProjectKey(projectId);

        return new JiraConnectionResponse(
                config.getProjectId(),
                config.getBaseUrl(),
                null,
                projectKey,
                JiraAuthType.API_TOKEN,
                true,
                config.getLastCheckedAt(),
                config.getStatus() == IntegrationConfigStatus.CONNECTED);
    }

    @Override
    public JiraConnectionResponse configureConnection(
            Long projectId,
            JiraConnectionRequest request) {

        if (projectId == null || projectId <= 0) {
            throw new JiraClientException(
                    "Project ID không hợp lệ");
        }

        if (request == null) {
            throw new JiraClientException(
                    "Jira connection request không được null");
        }

        String encryptedSecret =
                secretService.encrypt(request.apiToken());

        IntegrationConfig config =
                configRepository
                        .findByProjectIdAndProvider(
                                projectId,
                                IntegrationProvider.JIRA)
                        .orElseGet(() ->
                                new IntegrationConfig(
                                        projectId,
                                        IntegrationProvider.JIRA,
                                        encryptedSecret));

        config.setBaseUrl(request.siteUrl());
        config.setAccountIdentifier(request.email());
        config.setEncryptedSecret(encryptedSecret);
        config.setStatus(
                IntegrationConfigStatus.NOT_CHECKED);
        config.setLastCheckedAt(null);
        config.setLastErrorCode(null);

        IntegrationConfig saved =
                configRepository.save(config);

        jdbcTemplate.update(
                "UPDATE projects SET jira_project_key = ? WHERE id = ?",
                request.projectKey(),
                projectId);

        return new JiraConnectionResponse(
                saved.getProjectId(),
                saved.getBaseUrl(),
                null,
                request.projectKey(),
                request.authType() != null
                        ? request.authType()
                        : JiraAuthType.API_TOKEN,
                true,
                saved.getLastCheckedAt(),
                null);
    }

    @Override
    public JiraConnectionTestResponse testConnection(
            Long projectId) {

        IntegrationConfig config =
                configRepository
                        .findByProjectIdAndProvider(
                                projectId,
                                IntegrationProvider.JIRA)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Jira integration config not found for project: "
                                                + projectId));

        String projectKey =
                getSavedJiraProjectKey(projectId);

        if (projectKey == null
                || projectKey.isBlank()) {

            throw new JiraClientException(
                    "Jira project key chưa được cấu hình");
        }

        Instant testedAt = Instant.now();

        try {

            JiraConnectionResult result =
                    jiraClient.testConnection(
                            projectId,
                            projectKey);

            config.setStatus(
                    IntegrationConfigStatus.CONNECTED);

            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode(null);

            configRepository.save(config);

            return new JiraConnectionTestResponse(
                    projectId,
                    result.connected(),
                    null,
                    result.projectName(),
                    result.projectId(),
                    result.projectKey(),
                    testedAt,
                    null,
                    "Kết nối Jira Cloud thành công");

        } catch (JiraAuthenticationException
                 | JiraAuthorizationException
                 | JiraProjectNotFoundException
                 | JiraClientException e) {

            config.setStatus(
                    IntegrationConfigStatus.CONNECTION_FAILED);

            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode(
                    e.getErrorCode());

            configRepository.save(config);

            return new JiraConnectionTestResponse(
                    projectId,
                    false,
                    null,
                    null,
                    null,
                    projectKey,
                    testedAt,
                    e.getErrorCode(),
                    e.getMessage());
        }
    }

    /*
     * ============================================================
     * CNPM-80
     * Sync local Task -> Jira
     * ============================================================
     */

    @Override
    public JiraTaskSyncResponse syncTask(
            Long projectId,
            Long taskId,
            String idempotencyKey) {

        return doSyncTask(
                projectId,
                taskId,
                idempotencyKey,
                false);
    }

    @Override
    public JiraTaskSyncResponse retryTaskSync(
            Long projectId,
            Long taskId,
            String idempotencyKey) {

        return doSyncTask(
                projectId,
                taskId,
                idempotencyKey,
                true);
    }

    private JiraTaskSyncResponse doSyncTask(
            Long projectId,
            Long taskId,
            String idempotencyKey,
            boolean retry) {

        validateIds(projectId, taskId);

        // Validate Idempotency-Key before loading the Task so invalid requests
        // fail with the API-contract error instead of ResourceNotFoundException.
        String normalizedKey = requireIdempotencyKey(idempotencyKey);

        // Lock the Task row for the whole sync decision path. This is required
        // to prevent concurrent requests from creating duplicate Jira Issues.
        Task task = taskRepository.findByIdForUpdate(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Task với ID: " + taskId));

        if (!projectId.equals(task.getProjectId())) {
            throw new ResourceNotFoundException(
                    "Task " + taskId + " không thuộc Project " + projectId);
        }

        String fingerprint = requestFingerprint(task, retry);

        /*
         * Idempotency được kiểm tra trước mọi remote side-effect.
         */
        var previousLog = syncLogRepository
                .findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                        projectId,
                        ENTITY_TYPE_TASK,
                        String.valueOf(taskId),
                        normalizedKey);

        if (previousLog.isPresent()) {
            SyncLog old = previousLog.get();

            if (old.getRequestFingerprint() != null
                    && !old.getRequestFingerprint().equals(fingerprint)) {
                throw idempotencyKeyReusedException();
            }

            if (old.getStatus() != SyncLogStatus.RUNNING) {
                return responseFromSyncLog(old, taskId);
            }

            throw new JiraClientException(
                    "Request đồng bộ với Idempotency-Key này đang được xử lý");
        }

        Object lock = TASK_LOCKS.computeIfAbsent(taskId, ignored -> new Object());

        synchronized (lock) {
            /*
             * Double-check idempotency sau khi lấy lock để chống hai request
             * đồng thời trong cùng JVM trước khi tạo Jira Issue.
             */
            var lockedPreviousLog = syncLogRepository
                    .findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                            projectId, ENTITY_TYPE_TASK, String.valueOf(taskId), normalizedKey);
            if (lockedPreviousLog.isPresent()) {
                SyncLog old = lockedPreviousLog.get();
                if (!fingerprint.equals(old.getRequestFingerprint())) {
                    throw idempotencyKeyReusedException();
                }
                if (old.getStatus() != SyncLogStatus.RUNNING) {
                    return responseFromSyncLog(old, taskId);
                }
                throw new JiraClientException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "SYNC_ALREADY_RUNNING",
                        true,
                        "Request đồng bộ với Idempotency-Key này đang được xử lý");
            }

            var mapped = jiraIssueRepository.findByTaskId(taskId);
            if (mapped.isPresent()) {
                JiraIssue issue = mapped.get();

                // Mapping đã tồn tại là nguồn sự thật để tránh tạo Jira Issue trùng.
                // Nếu mapping cũ chưa có snapshotHash thì không thể kết luận dữ liệu
                // đã thay đổi; giữ nguyên mapping thay vì tạo Issue mới.
                if (issue.getSnapshotHash() == null
                        || fingerprint.equals(issue.getSnapshotHash())) {
                    if (task.getSyncStatus() != SyncStatus.SYNCED) {
                        task.setSyncStatus(SyncStatus.SYNCED);
                        taskRepository.save(task);
                    }
                    return buildSyncedResponse(
                            taskId, issue, "Task đã được đồng bộ lên Jira trước đó");
                }

                /*
                 * Task đã SYNCED nhưng dữ liệu local thay đổi:
                 * update remote issue thay vì tạo issue mới.
                 */
                String correlationId = UUID.randomUUID().toString();
                Instant startedAt = Instant.now();
                SyncLog syncLog = new SyncLog(
                        projectId, IntegrationProvider.JIRA, ENTITY_TYPE_TASK,
                        String.valueOf(taskId), SyncDirection.EXPORT,
                        correlationId, startedAt);
                syncLog.setRetryCount(retry ? 1 : 0);
                syncLog.setIdempotencyKey(normalizedKey);
                syncLog.setRequestFingerprint(fingerprint);
                syncLogRepository.saveAndFlush(syncLog);

                try {
                    JiraCreateIssueRequest updateRequest =
                            buildCreateRequest(task, taskId);

                    jiraClient.updateIssue(
                            projectId,
                            issue.getJiraIssueId(),
                            updateRequest);

                    Instant syncedAt = Instant.now();
                    issue.setLastSyncedAt(syncedAt);
                    issue.setSnapshotHash(fingerprint);
                    issue.setRawSnapshot(snapshot(task));
                    jiraIssueRepository.saveAndFlush(issue);

                    task.setSyncStatus(SyncStatus.SYNCED);
                    task.setIdempotencyKey(normalizedKey);
                    taskRepository.save(task);

                    syncLog.setStatus(SyncLogStatus.SUCCESS);
                    syncLog.setCompletedAt(Instant.now());
                    syncLogRepository.save(syncLog);

                    return buildSyncedResponse(
                            taskId, issue,
                            "Task đã thay đổi và đã được cập nhật lên Jira");
                } catch (Exception exception) {
                    syncLog.setStatus(SyncLogStatus.FAILED);
                    syncLog.setErrorCode(extractErrorCode(exception));
                    syncLog.setErrorMessage(safeErrorMessage(exception));
                    syncLog.setCompletedAt(Instant.now());
                    syncLogRepository.save(syncLog);
                    throw exception;
                }
            }

            if (task.getSyncStatus() == SyncStatus.SYNCING) {
                throw new JiraClientException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "SYNC_ALREADY_RUNNING",
                        true,
                        "Task đang trong quá trình đồng bộ");
            }

            if (retry
                    && task.getSyncStatus() != SyncStatus.SYNC_FAILED
                    && task.getSyncStatus() != SyncStatus.NOT_SYNCED) {
                throw new JiraClientException(
                        "Task không ở trạng thái có thể retry");
            }

            IntegrationConfig config = getConfig(projectId);
            String projectKey = requireProjectKey(projectId);
            String label = "cnpm-local-task-" + taskId;

            /*
             * Jira có thể đã tạo Issue nhưng local mapping chưa lưu được.
             * Kiểm tra label trước khi create để reconcile.
             */
            List<JiraCreateIssueResponse> discoveredIssues =
                    jiraClient.findIssuesByLabel(projectId, projectKey, label);
            if (discoveredIssues.size() > 1) {
                throw duplicateRemoteIssueException(label);
            }
            JiraCreateIssueResponse discovered =
                    discoveredIssues.isEmpty() ? null : discoveredIssues.get(0);

            String correlationId = UUID.randomUUID().toString();
            Instant startedAt = Instant.now();

            SyncLog syncLog = new SyncLog(
                    projectId,
                    IntegrationProvider.JIRA,
                    ENTITY_TYPE_TASK,
                    String.valueOf(taskId),
                    SyncDirection.EXPORT,
                    correlationId,
                    startedAt);
            syncLog.setRetryCount(retry ? 1 : 0);
            syncLog.setIdempotencyKey(normalizedKey);
            syncLog.setRequestFingerprint(fingerprint);
            try {
                syncLogRepository.save(syncLog);
            } catch (DataIntegrityViolationException concurrentRequest) {
                SyncLog existing = syncLogRepository
                        .findFirstByProjectIdAndEntityTypeAndEntityIdAndIdempotencyKeyOrderByStartedAtDesc(
                                projectId, ENTITY_TYPE_TASK, String.valueOf(taskId), normalizedKey)
                        .orElseThrow(() -> concurrentRequest);
                if (!fingerprint.equals(existing.getRequestFingerprint())) {
                    throw idempotencyKeyReusedException();
                }
                if (existing.getStatus() != SyncLogStatus.RUNNING) {
                    return responseFromSyncLog(existing, taskId);
                }
                throw new JiraClientException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "SYNC_ALREADY_RUNNING",
                        true,
                        "Request đồng bộ với Idempotency-Key này đang được xử lý");
            }

            task.setIdempotencyKey(normalizedKey);
            task.setSyncStatus(SyncStatus.SYNCING);
            taskRepository.save(task);

            try {
                JiraCreateIssueResponse jiraResponse = discovered;

                if (jiraResponse == null) {
                    JiraCreateIssueRequest request =
                            buildCreateRequest(task, taskId);

                    jiraResponse = jiraClient.createIssue(
                            projectId,
                            projectKey,
                            request);
                }

                validateJiraCreateResponse(jiraResponse);

                Instant syncedAt = Instant.now();
                String jiraIssueUrl = buildJiraIssueUrl(
                        config.getBaseUrl(),
                        jiraResponse.key());

                JiraIssue jiraIssue = new JiraIssue(
                        taskId,
                        jiraResponse.id(),
                        jiraResponse.key(),
                        jiraIssueUrl,
                        syncedAt);
                jiraIssue.setSnapshotHash(fingerprint);
                jiraIssue.setRawSnapshot(snapshot(task));

                try {
                    jiraIssueRepository.saveAndFlush(jiraIssue);
                } catch (DataIntegrityViolationException duplicate) {
                    /*
                     * Request khác có thể vừa lưu mapping. Đọc lại thay vì
                     * tạo thêm Issue.
                     */
                    JiraIssue existing = jiraIssueRepository
                            .findByTaskId(taskId)
                            .orElseThrow(() -> duplicate);
                    jiraIssue = existing;
                }

                task.setSyncStatus(SyncStatus.SYNCED);
                task.setIdempotencyKey(normalizedKey);
                taskRepository.save(task);

                syncLog.setStatus(SyncLogStatus.SUCCESS);
                syncLog.setCompletedAt(Instant.now());
                syncLogRepository.save(syncLog);

                return new JiraTaskSyncResponse(
                        taskId,
                        SyncStatus.SYNCED,
                        jiraResponse.id(),
                        jiraResponse.key(),
                        jiraIssueUrl,
                        retry ? 2 : 1,
                        false,
                        syncedAt,
                        null,
                        retry
                                ? "Retry đồng bộ Jira thành công"
                                : "Đồng bộ Task lên Jira thành công");

            } catch (Exception exception) {
                /*
                 * Nếu create đã thành công nhưng response bị timeout/mất kết nối,
                 * hoặc local save gặp lỗi, luôn reconcile bằng label trước.
                 */
                if (!isRetryable(exception) && !(exception instanceof DataIntegrityViolationException)) {
                    task.setSyncStatus(SyncStatus.SYNC_FAILED);
                    taskRepository.save(task);
                    syncLog.setStatus(SyncLogStatus.FAILED);
                    syncLog.setErrorCode(extractErrorCode(exception));
                    syncLog.setErrorMessage(safeErrorMessage(exception));
                    syncLog.setCompletedAt(Instant.now());
                    syncLogRepository.save(syncLog);
                    throw exception;
                }

                try {
                    List<JiraCreateIssueResponse> reconciledIssues =
                            jiraClient.findIssuesByLabel(
                                    projectId, projectKey, label);
                    if (reconciledIssues.size() > 1) {
                        throw duplicateRemoteIssueException(label);
                    }
                    JiraCreateIssueResponse reconciled =
                            reconciledIssues.isEmpty() ? null : reconciledIssues.get(0);

                    if (reconciled != null) {
                        Instant syncedAt = Instant.now();
                        String url = buildJiraIssueUrl(
                                config.getBaseUrl(), reconciled.key());

                        JiraIssue issue = new JiraIssue(
                                taskId,
                                reconciled.id(),
                                reconciled.key(),
                                url,
                                syncedAt);
                        issue.setSnapshotHash(fingerprint);
                        issue.setRawSnapshot(snapshot(task));

                        try {
                            jiraIssueRepository.saveAndFlush(issue);
                        } catch (DataIntegrityViolationException ignored) {
                            issue = jiraIssueRepository.findByTaskId(taskId)
                                    .orElse(issue);
                        }

                        task.setSyncStatus(SyncStatus.SYNCED);
                        task.setIdempotencyKey(normalizedKey);
                        taskRepository.save(task);

                        syncLog.setStatus(SyncLogStatus.SUCCESS);
                        syncLog.setCompletedAt(Instant.now());
                        syncLogRepository.save(syncLog);

                        return new JiraTaskSyncResponse(
                                taskId,
                                SyncStatus.SYNCED,
                                reconciled.id(),
                                reconciled.key(),
                                url,
                                retry ? 2 : 1,
                                false,
                                syncedAt,
                                null,
                                "Đã tìm thấy Jira Issue sau khi reconcile");
                    }
                } catch (Exception ignored) {
                    // Giữ lỗi gốc.
                }

                task.setSyncStatus(SyncStatus.SYNC_FAILED);
                taskRepository.save(task);

                syncLog.setStatus(SyncLogStatus.FAILED);
                syncLog.setErrorCode(extractErrorCode(exception));
                syncLog.setErrorMessage(safeErrorMessage(exception));
                syncLog.setCompletedAt(Instant.now());
                syncLogRepository.save(syncLog);

                return new JiraTaskSyncResponse(
                        taskId,
                        SyncStatus.SYNC_FAILED,
                        null,
                        null,
                        null,
                        retry ? 2 : 1,
                        isRetryable(exception),
                        null,
                        extractErrorCode(exception),
                        safeErrorMessage(exception));
            }
        }
    }

    private IntegrationConfig getConfig(Long projectId) {
        return configRepository
                .findByProjectIdAndProvider(
                        projectId, IntegrationProvider.JIRA)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Jira integration chưa được cấu hình cho Project: " + projectId));
    }

    private String requireProjectKey(Long projectId) {
        String key = getSavedJiraProjectKey(projectId);
        if (key == null || key.isBlank()) {
            throw new JiraClientException("Jira project key chưa được cấu hình");
        }
        return key;
    }

    private JiraCreateIssueRequest buildCreateRequest(Task task, Long taskId) {
        String assigneeEmail = null;
        String sprintId = null;
        String epicKey = null;

        try {
            if (task.getAssigneeUserId() != null) {
                var emails = jdbcTemplate.query(
                        "SELECT email FROM users WHERE id = ?",
                        (rs, rowNum) -> rs.getString("email"),
                        task.getAssigneeUserId());
                if (!emails.isEmpty()) {
                    assigneeEmail = emails.get(0);
                }
            }

            if (task.getSprintId() != null) {
                var sprintIds = jdbcTemplate.query(
                        "SELECT jira_sprint_id FROM sprints WHERE id = ? AND project_id = ?",
                        (rs, rowNum) -> {
                            long value = rs.getLong("jira_sprint_id");
                            return rs.wasNull() ? null : String.valueOf(value);
                        },
                        task.getSprintId(), task.getProjectId());
                if (!sprintIds.isEmpty()) {
                    sprintId = sprintIds.get(0);
                }
            }

            if (task.getFeatureId() != null) {
                var epicKeys = jdbcTemplate.query(
                        "SELECT jira_epic_key FROM features WHERE id = ? AND project_id = ?",
                        (rs, rowNum) -> rs.getString("jira_epic_key"),
                        task.getFeatureId(), task.getProjectId());
                if (!epicKeys.isEmpty()) {
                    epicKey = epicKeys.get(0);
                }
            }
        } catch (Exception e) {
            throw new JiraClientException(
                    "Không thể resolve dữ liệu mapping Task sang Jira",
                    e);
        }

        return new JiraCreateIssueRequest(
                task.getTitle(),
                task.getDescription(),
                task.getIssueType() == null ? null : task.getIssueType().name(),
                task.getPriority() == null ? null : task.getPriority().name(),
                List.of("cnpm-local-task-" + taskId),
                assigneeEmail,
                task.getDeadline() == null ? null : task.getDeadline().toString(),
                sprintId,
                epicKey);
    }

    private Map<String, Object> snapshot(Task task) {
        return Map.of(
                "title", task.getTitle() == null ? "" : task.getTitle(),
                "description", task.getDescription() == null ? "" : task.getDescription(),
                "issueType", task.getIssueType() == null ? "" : task.getIssueType().name(),
                "priority", task.getPriority() == null ? "" : task.getPriority().name(),
                "assigneeUserId", task.getAssigneeUserId() == null ? "" : task.getAssigneeUserId().toString(),
                "deadline", task.getDeadline() == null ? "" : task.getDeadline().toString(),
                "sprintId", task.getSprintId() == null ? "" : task.getSprintId().toString(),
                "featureId", task.getFeatureId() == null ? "" : task.getFeatureId().toString());
    }

    private String requestFingerprint(Task task, boolean retry) {
        String value = String.join("|",
                retry ? "RETRY" : "SYNC",
                "mapping-v1",
                task.getTitle() == null ? "" : task.getTitle(),
                task.getDescription() == null ? "" : task.getDescription(),
                task.getAcceptanceCriteria() == null ? "" : task.getAcceptanceCriteria(),
                task.getIssueType() == null ? "" : task.getIssueType().name(),
                task.getPriority() == null ? "" : task.getPriority().name(),
                task.getAssigneeUserId() == null ? "" : task.getAssigneeUserId().toString(),
                task.getDeadline() == null ? "" : task.getDeadline().toString(),
                task.getSprintId() == null ? "" : task.getSprintId().toString(),
                task.getFeatureId() == null ? "" : task.getFeatureId().toString());

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new JiraClientException("Không thể tạo request fingerprint", e);
        }
    }

    private JiraTaskSyncResponse responseFromSyncLog(
            SyncLog log,
            Long taskId) {

        if (log.getStatus() == SyncLogStatus.SUCCESS) {
            var issue = jiraIssueRepository.findByTaskId(taskId);
            if (issue.isPresent()) {
                return buildSyncedResponse(
                        taskId,
                        issue.get(),
                        "Kết quả idempotent cũ");
            }
        }

        SyncStatus status =
                log.getStatus() == SyncLogStatus.SUCCESS
                        ? SyncStatus.SYNCED
                        : SyncStatus.SYNC_FAILED;

        return new JiraTaskSyncResponse(
                taskId,
                status,
                null,
                null,
                null,
                log.getRetryCount() + 1,
                status == SyncStatus.SYNC_FAILED,
                log.getCompletedAt(),
                log.getErrorCode(),
                log.getErrorMessage() == null
                        ? "Kết quả idempotent cũ"
                        : log.getErrorMessage());
    }

    private void validateIds(
            Long projectId,
            Long taskId) {

        if (projectId == null
                || projectId <= 0) {

            throw new JiraClientException(
                    "Project ID không hợp lệ");
        }

        if (taskId == null
                || taskId <= 0) {

            throw new JiraClientException(
                    "Task ID không hợp lệ");
        }
    }

    private void validateJiraCreateResponse(
            JiraCreateIssueResponse response) {

        if (response == null) {
            throw new JiraConnectionException(
                    "Jira không trả về create issue response");
        }

        if (response.id() == null
                || response.id().isBlank()) {

            throw new JiraConnectionException(
                    "Jira response thiếu issue id");
        }

        if (response.key() == null
                || response.key().isBlank()) {

            throw new JiraConnectionException(
                    "Jira response thiếu issue key");
        }
    }

    private JiraTaskSyncResponse buildSyncedResponse(
            Long taskId,
            JiraIssue issue,
            String message) {

        return new JiraTaskSyncResponse(
                taskId,
                SyncStatus.SYNCED,
                issue.getJiraIssueId(),
                issue.getJiraIssueKey(),
                issue.getUrl(),
                0,
                false,
                issue.getLastSyncedAt(),
                null,
                message);
    }

    private JiraTaskSyncResponse buildAlreadySyncedResponse(
            Task task) {

        var issue =
                jiraIssueRepository
                        .findByTaskId(task.getId());

        if (issue.isPresent()) {

            return buildSyncedResponse(
                    task.getId(),
                    issue.get(),
                    "Task đã được đồng bộ lên Jira");
        }

        /*
         * Task đang SYNCED nhưng không có mapping.
         *
         * Không tự ý tạo Jira Issue mới.
         */
        return new JiraTaskSyncResponse(
                task.getId(),
                SyncStatus.SYNCED,
                null,
                null,
                null,
                0,
                false,
                null,
                null,
                "Task đã ở trạng thái SYNCED");
    }

    private JiraApiException idempotencyKeyReusedException() {
        return new JiraApiException(
                org.springframework.http.HttpStatus.CONFLICT,
                "IDEMPOTENCY_KEY_REUSED",
                false,
                null,
                "Idempotency-Key đã được sử dụng cho request khác",
                null);
    }

    private JiraApiException duplicateRemoteIssueException(String label) {
        return new JiraApiException(
                org.springframework.http.HttpStatus.CONFLICT,
                "DUPLICATE_REMOTE_ISSUE",
                false,
                null,
                "Có nhiều Jira Issue cùng label " + label,
                null);
    }

    private String requireIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency-Key là bắt buộc");
        }
        return normalizeIdempotencyKey(idempotencyKey);
    }

    private String normalizeIdempotencyKey(
            String idempotencyKey) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            return null;
        }

        String normalized =
                idempotencyKey.trim();

        if (normalized.length() < 8 || normalized.length() > 100) {
            throw new JiraClientException(
                    "Idempotency key phải có độ dài từ 8 đến 100 ký tự");
        }

        return normalized;
    }

    private boolean isRetryable(
            Exception exception) {

        if (exception instanceof JiraApiException jiraException) {
            return jiraException.isRetryable();
        }

        return exception instanceof JiraConnectionException;
    }

    private String extractErrorCode(
            Exception exception) {

        if (exception instanceof JiraApiException jiraException) {
            return jiraException.getErrorCode();
        }

        return exception.getClass().getSimpleName();
    }

    private String safeErrorMessage(
            Exception exception) {

        if (exception.getMessage() == null
                || exception.getMessage().isBlank()) {

            return "Jira synchronization failed";
        }

        return exception.getMessage();
    }

    private String buildJiraIssueUrl(
            String baseUrl,
            String issueKey) {

        if (baseUrl == null
                || baseUrl.isBlank()
                || issueKey == null
                || issueKey.isBlank()) {

            throw new JiraClientException(
                    "Không thể tạo Jira issue URL");
        }

        return baseUrl.replaceAll("/+$", "")
                + "/browse/"
                + issueKey;
    }

    @Override
    @Transactional(readOnly = true)
    public JiraIssueResponse getIssue(
            Long projectId,
            String jiraIssueKey) {

        if (projectId == null
                || projectId <= 0) {

            throw new JiraClientException(
                    "Project ID không hợp lệ");
        }

        if (jiraIssueKey == null
                || jiraIssueKey.isBlank()) {

            throw new JiraClientException(
                    "Jira issue key không được để trống");
        }

        /*
         * Đảm bảo issue thuộc mapping của project.
         */
        var issue =
                jiraIssueRepository
                        .findByJiraIssueKey(
                                jiraIssueKey.trim());

        if (issue.isEmpty()) {

            throw new ResourceNotFoundException(
                    "Không tìm thấy Jira Issue: "
                            + jiraIssueKey);
        }

        JiraIssue jiraIssue =
                issue.get();

        Task task =
                taskRepository
                        .findById(jiraIssue.getTaskId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy Task của Jira Issue: "
                                                + jiraIssueKey));

        if (!projectId.equals(task.getProjectId())) {

            throw new ResourceNotFoundException(
                    "Jira Issue không thuộc Project "
                            + projectId);
        }

        return new JiraIssueResponse(
                projectId,
                jiraIssue.getTaskId(),
                jiraIssue.getJiraIssueId(),
                jiraIssue.getJiraIssueKey(),
                jiraIssue.getUrl(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                jiraIssue.getRemoteUpdatedAt(),
                jiraIssue.getLastSyncedAt());
    }

    private String getSavedJiraProjectKey(
            Long projectId) {

        try {

            var keys =
                    jdbcTemplate.query(
                            "SELECT jira_project_key "
                                    + "FROM projects "
                                    + "WHERE id = ?",
                            (rs, rowNum) ->
                                    rs.getString(
                                            "jira_project_key"),
                            projectId);

            if (!keys.isEmpty()
                    && keys.get(0) != null
                    && !keys.get(0).isBlank()) {

                return keys.get(0).trim();
            }

        } catch (Exception ignored) {
            /*
             * Không làm hỏng toàn bộ service vì lỗi đọc
             * project key.
             */
        }

        return null;
    }
}

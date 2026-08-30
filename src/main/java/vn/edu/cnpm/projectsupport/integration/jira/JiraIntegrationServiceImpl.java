package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Instant;
import java.util.UUID;

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

        /*
         * ========================================================
         * 1. Lấy Task
         * ========================================================
         */

        Task task =
                taskRepository
                        .findById(taskId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Không tìm thấy Task với ID: "
                                                + taskId));

        if (!projectId.equals(task.getProjectId())) {
            throw new ResourceNotFoundException(
                    "Task "
                            + taskId
                            + " không thuộc Project "
                            + projectId);
        }

        /*
         * ========================================================
         * 2. Mapping check
         *
         * Nếu JiraIssue đã tồn tại cho Task:
         * - Không gọi Jira createIssue()
         * - Đảm bảo Task = SYNCED
         * - Trả mapping cũ
         *
         * Đây là lớp chống duplicate quan trọng nhất.
         * ========================================================
         */

        var existingIssue =
                jiraIssueRepository.findByTaskId(taskId);

        if (existingIssue.isPresent()) {

            JiraIssue issue =
                    existingIssue.get();

            if (task.getSyncStatus() != SyncStatus.SYNCED) {
                task.setSyncStatus(SyncStatus.SYNCED);
                taskRepository.save(task);
            }

            return buildSyncedResponse(
                    taskId,
                    issue,
                    "Task đã được đồng bộ lên Jira trước đó");
        }

        /*
         * ========================================================
         * 3. Kiểm tra trạng thái Task
         * ========================================================
         */

        if (!retry
                && task.getSyncStatus() == SyncStatus.SYNCED) {

            return buildAlreadySyncedResponse(task);
        }

        if (task.getSyncStatus() == SyncStatus.SYNCING) {

            throw new JiraClientException(
                    "Task đang trong quá trình đồng bộ");
        }

        if (retry
                && task.getSyncStatus() != SyncStatus.SYNC_FAILED
                && task.getSyncStatus() != SyncStatus.NOT_SYNCED) {

            throw new JiraClientException(
                    "Task không ở trạng thái có thể retry");
        }

        /*
         * ========================================================
         * 4. Lấy IntegrationConfig
         *
         * BẮT BUỘC theo projectId + provider.
         * Không dùng Jira config toàn cục.
         * ========================================================
         */

        IntegrationConfig config =
                configRepository
                        .findByProjectIdAndProvider(
                                projectId,
                                IntegrationProvider.JIRA)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Jira integration chưa được cấu hình cho Project: "
                                                + projectId));

        String projectKey =
                getSavedJiraProjectKey(projectId);

        if (projectKey == null
                || projectKey.isBlank()) {

            throw new JiraClientException(
                    "Jira project key chưa được cấu hình");
        }

        /*
         * ========================================================
         * 5. Idempotency key
         *
         * Ưu tiên:
         *   request key
         *   -> key đã lưu trên Task
         *   -> key deterministic theo Task
         *
         * Không tạo UUID mới mỗi lần retry.
         * ========================================================
         */

        String normalizedKey =
                normalizeIdempotencyKey(idempotencyKey);

        if (normalizedKey == null) {
            normalizedKey =
                    normalizeIdempotencyKey(
                            task.getIdempotencyKey());
        }

        if (normalizedKey == null) {
            normalizedKey =
                    "task-" + taskId + "-jira";
        }

        if (task.getIdempotencyKey() == null
                || task.getIdempotencyKey().isBlank()) {

            task.setIdempotencyKey(normalizedKey);
            taskRepository.save(task);
        }

        /*
         * ========================================================
         * 6. Tạo SyncLog cho MỖI lần sync/retry
         * ========================================================
         */

        String correlationId =
                UUID.randomUUID().toString();

        Instant startedAt =
                Instant.now();

        SyncLog syncLog =
                new SyncLog(
                        projectId,
                        IntegrationProvider.JIRA,
                        ENTITY_TYPE_TASK,
                        String.valueOf(taskId),
                        SyncDirection.EXPORT,
                        correlationId,
                        startedAt);

        syncLog.setRetryCount(
                retry ? 1 : 0);

        syncLogRepository.save(syncLog);

        /*
         * ========================================================
         * 7. NOT_SYNCED / SYNC_FAILED -> SYNCING
         *
         * Lưu trạng thái trước khi gọi Jira.
         * ========================================================
         */

        task.setSyncStatus(
                SyncStatus.SYNCING);

        taskRepository.save(task);

        /*
         * ========================================================
         * 8. Gọi Jira createIssue()
         * ========================================================
         */

        try {

            JiraCreateIssueRequest request =
                    new JiraCreateIssueRequest(
                            task.getTitle(),
                            task.getDescription(),
                            task.getIssueType().name(),
                            task.getPriority().name());

            JiraCreateIssueResponse jiraResponse =
                    jiraClient.createIssue(
                            projectId,
                            projectKey,
                            request);

            validateJiraCreateResponse(
                    jiraResponse);

            Instant syncedAt =
                    Instant.now();

            String jiraIssueUrl =
                    buildJiraIssueUrl(
                            config.getBaseUrl(),
                            jiraResponse.key());

            /*
             * ====================================================
             * 9. Lưu JiraIssue mapping
             * ====================================================
             */

            JiraIssue jiraIssue =
                    new JiraIssue(
                            taskId,
                            jiraResponse.id(),
                            jiraResponse.key(),
                            jiraIssueUrl,
                            syncedAt);

            jiraIssueRepository.saveAndFlush(
                    jiraIssue);

            /*
             * ====================================================
             * 10. Task -> SYNCED
             * ====================================================
             */

            task.setSyncStatus(
                    SyncStatus.SYNCED);

            taskRepository.save(task);

            /*
             * ====================================================
             * 11. SyncLog -> SUCCESS
             * ====================================================
             */

            syncLog.setStatus(
                    SyncLogStatus.SUCCESS);

            syncLog.setCompletedAt(
                    Instant.now());

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
             * ====================================================
             * 12. Jira thất bại
             *
             * QUAN TRỌNG:
             * - Không xóa Task
             * - Không chuyển Task về NOT_SYNCED
             * - Chuyển sang SYNC_FAILED
             * - Ghi SyncLog FAILED
             * ====================================================
             */

            task.setSyncStatus(
                    SyncStatus.SYNC_FAILED);

            taskRepository.save(task);

            syncLog.setStatus(
                    SyncLogStatus.FAILED);

            syncLog.setErrorCode(
                    extractErrorCode(exception));

            syncLog.setErrorMessage(
                    safeErrorMessage(exception));

            syncLog.setCompletedAt(
                    Instant.now());

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

    private String normalizeIdempotencyKey(
            String idempotencyKey) {

        if (idempotencyKey == null
                || idempotencyKey.isBlank()) {

            return null;
        }

        String normalized =
                idempotencyKey.trim();

        if (normalized.length() > 100) {

            throw new JiraClientException(
                    "Idempotency key không được vượt quá 100 ký tự");
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
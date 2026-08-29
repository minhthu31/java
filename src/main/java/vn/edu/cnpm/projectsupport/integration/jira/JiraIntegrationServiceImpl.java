package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Instant;
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
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
@Transactional
public class JiraIntegrationServiceImpl implements JiraIntegrationService {

    private final IntegrationConfigRepository configRepository;
    private final IntegrationSecretService secretService;
    private final JiraClient jiraClient;
    private final JdbcTemplate jdbcTemplate;

    public JiraIntegrationServiceImpl(
            IntegrationConfigRepository configRepository,
            IntegrationSecretService secretService,
            JiraClient jiraClient,
            JdbcTemplate jdbcTemplate) {
        this.configRepository = configRepository;
        this.secretService = secretService;
        this.jiraClient = jiraClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public JiraConnectionResponse getConnection(Long projectId) {
        IntegrationConfig config = configRepository
                .findByProjectIdAndProvider(projectId, IntegrationProvider.JIRA)
                .orElse(null);

        if (config == null) {
            return new JiraConnectionResponse(
                    projectId, null, null, null, null, false, null, null);
        }

        // Điểm 1: Lấy đúng projectKey của project để GET config không bị null
        String projectKey = resolveProjectKey(projectId);

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
    public JiraConnectionResponse configureConnection(Long projectId, JiraConnectionRequest request) {
        String encryptedSecret = secretService.encrypt(request.apiToken());

        IntegrationConfig config = configRepository
                .findByProjectIdAndProvider(projectId, IntegrationProvider.JIRA)
                .orElseGet(() -> new IntegrationConfig(projectId, IntegrationProvider.JIRA, encryptedSecret));

        config.setBaseUrl(request.siteUrl());
        // Chỉ lưu email vào account_identifier để Basic Auth không bị sai username
        config.setAccountIdentifier(request.email());
        config.setEncryptedSecret(encryptedSecret);
        config.setStatus(IntegrationConfigStatus.NOT_CHECKED);
        config.setLastCheckedAt(null);
        config.setLastErrorCode(null);

        IntegrationConfig saved = configRepository.save(config);

        return new JiraConnectionResponse(
                saved.getProjectId(),
                saved.getBaseUrl(),
                null,
                request.projectKey(),
                request.authType() != null ? request.authType() : JiraAuthType.API_TOKEN,
                true,
                saved.getLastCheckedAt(),
                null);
    }

    @Override
    public JiraConnectionTestResponse testConnection(Long projectId) {
        IntegrationConfig config = configRepository
                .findByProjectIdAndProvider(projectId, IntegrationProvider.JIRA)
                .orElseThrow(() -> new ResourceNotFoundException("Jira integration config not found for project: " + projectId));

        String projectKey = resolveProjectKey(projectId);
        Instant testedAt = Instant.now();

        try {
            // Điểm 2: Gọi JiraClient với đúng projectKey thật, không truyền null
            JiraConnectionResult result = jiraClient.testConnection(projectId, projectKey);

            config.setStatus(IntegrationConfigStatus.CONNECTED);
            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode(null);
            configRepository.save(config);

            // Điểm 3: Map đúng chuẩn theo OpenAPI contract
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
        } catch (JiraAuthenticationException | JiraAuthorizationException | JiraProjectNotFoundException | JiraClientException e) {
            // Điểm 4: Chỉ bắt lỗi credential / authorization / not found và trả kết quả test thất bại (200)
            config.setStatus(IntegrationConfigStatus.CONNECTION_FAILED);
            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode(e.getErrorCode());
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
        // Lỗi mạng/502 (JiraConnectionException) và 429 (JiraRateLimitException) sẽ throw ra ngoài cho GlobalExceptionHandler
    }

    @Override
    public JiraTaskSyncResponse syncTask(Long projectId, Long taskId, String idempotencyKey) {
        throw new UnsupportedOperationException("syncTask is handled in subsequent tasks");
    }

    @Override
    public JiraTaskSyncResponse retryTaskSync(Long projectId, Long taskId, String idempotencyKey) {
        throw new UnsupportedOperationException("retryTaskSync is handled in subsequent tasks");
    }

    @Override
    public JiraIssueResponse getIssue(Long projectId, String jiraIssueKey) {
        throw new UnsupportedOperationException("getIssue is handled in subsequent tasks");
    }

    private String resolveProjectKey(Long projectId) {
        try {
            var keys = jdbcTemplate.query(
                    "SELECT g.code FROM student_groups g JOIN projects p ON p.group_id = g.id WHERE p.id = ?",
                    (rs, rowNum) -> rs.getString("code"),
                    projectId);
            if (!keys.isEmpty() && keys.get(0) != null && !keys.get(0).isBlank()) {
                return keys.get(0);
            }
        } catch (Exception ignored) {
        }
        return "CNPM";
    }
}
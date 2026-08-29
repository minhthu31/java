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

        String projectKey = getSavedJiraProjectKey(projectId);

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
        config.setAccountIdentifier(request.email());
        config.setEncryptedSecret(encryptedSecret);
        config.setStatus(IntegrationConfigStatus.NOT_CHECKED);
        config.setLastCheckedAt(null);
        config.setLastErrorCode(null);

        IntegrationConfig saved = configRepository.save(config);

        // Lưu trực tiếp request.projectKey() vào projects.jira_project_key
        jdbcTemplate.update(
                "UPDATE projects SET jira_project_key = ? WHERE id = ?",
                request.projectKey(),
                projectId);

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

        String projectKey = getSavedJiraProjectKey(projectId);
        if (projectKey == null || projectKey.isBlank()) {
            throw new JiraClientException("Jira project key chưa được cấu hình");
        }

        Instant testedAt = Instant.now();
        try {
            JiraConnectionResult result = jiraClient.testConnection(projectId, projectKey);

            config.setStatus(IntegrationConfigStatus.CONNECTED);
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
        } catch (JiraAuthenticationException | JiraAuthorizationException | JiraProjectNotFoundException | JiraClientException e) {
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

    private String getSavedJiraProjectKey(Long projectId) {
        try {
            var keys = jdbcTemplate.query(
                    "SELECT jira_project_key FROM projects WHERE id = ?",
                    (rs, rowNum) -> rs.getString("jira_project_key"),
                    projectId);
            if (!keys.isEmpty() && keys.get(0) != null && !keys.get(0).isBlank()) {
                return keys.get(0);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
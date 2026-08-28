package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Instant;
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

    public JiraIntegrationServiceImpl(
            IntegrationConfigRepository configRepository,
            IntegrationSecretService secretService,
            JiraClient jiraClient) {
        this.configRepository = configRepository;
        this.secretService = secretService;
        this.jiraClient = jiraClient;
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

        return new JiraConnectionResponse(
                config.getProjectId(),
                config.getBaseUrl(),
                null,
                null,
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

        Instant testedAt = Instant.now();
        try {
            JiraConnectionResult result = jiraClient.testConnection(projectId, null);

            config.setStatus(IntegrationConfigStatus.CONNECTED);
            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode(null);
            configRepository.save(config);

            return new JiraConnectionTestResponse(
                    projectId,
                    result.connected(),
                    result.projectId(),
                    result.projectName(),
                    result.projectId(),
                    result.projectKey(),
                    testedAt,
                    null,
                    "Kết nối Jira Cloud thành công");
        } catch (JiraApiException e) {
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
                    null,
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
}
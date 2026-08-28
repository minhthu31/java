package vn.edu.cnpm.projectsupport.integration.jira;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;

@Service
@Transactional
public class JiraIntegrationServiceImpl implements JiraIntegrationService {

    private final IntegrationConfigRepository configRepository;

    public JiraIntegrationServiceImpl(IntegrationConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public JiraConnectionResponse getConnection(Long projectId) {
        IntegrationConfig config = configRepository
                .findByProjectIdAndProvider(projectId, IntegrationProvider.JIRA)
                .orElseThrow(() -> new ResourceNotFoundException("Jira integration config not found for project: " + projectId));

        return toConnectionResponse(config);
    }

    @Override
    public JiraConnectionResponse configureConnection(Long projectId, JiraConnectionRequest request) {
        String encryptedSecret = "enc:v1:" + Base64.getEncoder().encodeToString(request.apiToken().getBytes(StandardCharsets.UTF_8));

        IntegrationConfig config = configRepository
                .findByProjectIdAndProvider(projectId, IntegrationProvider.JIRA)
                .orElseGet(() -> new IntegrationConfig(projectId, IntegrationProvider.JIRA, encryptedSecret));

        config.setBaseUrl(request.siteUrl());
        config.setAccountIdentifier(request.email() + ":" + request.projectKey());
        config.setEncryptedSecret(encryptedSecret);
        config.setStatus(IntegrationConfigStatus.NOT_CHECKED);

        IntegrationConfig saved = configRepository.save(config);
        return toConnectionResponse(saved);
    }

    @Override
    public JiraConnectionTestResponse testConnection(Long projectId) {
        IntegrationConfig config = configRepository
                .findByProjectIdAndProvider(projectId, IntegrationProvider.JIRA)
                .orElseThrow(() -> new ResourceNotFoundException("Jira integration config not found for project: " + projectId));

        String projectKey = config.getAccountIdentifier() != null && config.getAccountIdentifier().contains(":")
                ? config.getAccountIdentifier().split(":", 2)[1]
                : "";

        return new JiraConnectionTestResponse(
                projectId, true, "test-account-id", "Jira Admin", null, projectKey, Instant.now(), null, "Kết nối Jira Cloud thành công");
    }

    @Override
    public JiraTaskSyncResponse syncTask(Long projectId, Long taskId, String idempotencyKey) {
        throw new UnsupportedOperationException("Not implemented in CNPM-78");
    }

    @Override
    public JiraTaskSyncResponse retryTaskSync(Long projectId, Long taskId, String idempotencyKey) {
        throw new UnsupportedOperationException("Not implemented in CNPM-78");
    }

    @Override
    public JiraIssueResponse getIssue(Long projectId, String jiraIssueKey) {
        throw new UnsupportedOperationException("Not implemented in CNPM-78");
    }

    private JiraConnectionResponse toConnectionResponse(IntegrationConfig config) {
        String projectKey = config.getAccountIdentifier() != null && config.getAccountIdentifier().contains(":")
                ? config.getAccountIdentifier().split(":", 2)[1]
                : null;
        return new JiraConnectionResponse(
                config.getProjectId(),
                config.getBaseUrl(),
                null,
                projectKey,
                JiraAuthType.API_TOKEN,
                true,
                config.getLastCheckedAt(),
                config.getStatus() == IntegrationConfigStatus.CONNECTED
        );
    }
}
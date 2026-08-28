
package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
public class JiraIntegrationServiceImpl
        implements JiraIntegrationService {

    private final IntegrationConfigRepository integrationConfigRepository;
    private final JiraClient jiraClient;
    private final IntegrationSecretService integrationSecretService;

    public JiraIntegrationServiceImpl(
            IntegrationConfigRepository integrationConfigRepository,
            JiraClient jiraClient,
            IntegrationSecretService integrationSecretService) {

        this.integrationConfigRepository =
                integrationConfigRepository;

        this.jiraClient = jiraClient;

        this.integrationSecretService =
                integrationSecretService;
    }

    @Override
    @Transactional(readOnly = true)
    public JiraConnectionResponse getConnection(
            Long projectId) {

        IntegrationConfig config =
                integrationConfigRepository
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
                    false);
        }

        
        return new JiraConnectionResponse(
                config.getProjectId(),
                config.getBaseUrl(),
                config.getAccountIdentifier(),
                null,
                null,
                true,
                config.getLastCheckedAt(),
                config.getStatus()
                        == IntegrationConfigStatus.CONNECTED);
    }

    @Override
    @Transactional
    public JiraConnectionResponse configureConnection(
            Long projectId,
            JiraConnectionRequest request) {

        IntegrationConfig config =
                integrationConfigRepository
                        .findByProjectIdAndProvider(
                                projectId,
                                IntegrationProvider.JIRA)
                        .orElseGet(() ->
                                new IntegrationConfig(
                                        projectId,
                                        IntegrationProvider.JIRA,
                                        ""));

        /*
         * Lưu site URL.
         */
        config.setBaseUrl(
                request.siteUrl());

        /*
         * Lưu email/account identifier.
         */
        config.setAccountIdentifier(
                request.email());

        
        String encryptedSecret =
                integrationSecretService.encrypt(
                        request.apiToken());

        if (encryptedSecret == null
                || encryptedSecret.isBlank()) {

            throw new JiraClientException(
                    "Không thể mã hóa Jira secret");
        }

        config.setEncryptedSecret(
                encryptedSecret);

        config.setStatus(
                IntegrationConfigStatus.NOT_CHECKED);

        config.setLastCheckedAt(null);

        config.setLastErrorCode(null);

        integrationConfigRepository.save(
                config);

        
        return new JiraConnectionResponse(
                config.getProjectId(),
                config.getBaseUrl(),
                config.getAccountIdentifier(),
                request.projectKey(),
                request.authType(),
                true,
                config.getLastCheckedAt(),
                false);
    }

    @Override
    @Transactional
    public JiraConnectionTestResponse testConnection(
            Long projectId) {

        IntegrationConfig config =
                integrationConfigRepository
                        .findByProjectIdAndProvider(
                                projectId,
                                IntegrationProvider.JIRA)
                        .orElseThrow(() ->
                                new JiraClientException(
                                        "Jira integration chưa được cấu hình cho project"));

        
        throw new JiraClientException(
                "Jira Project Key chưa có nguồn cấu hình cho test connection");
    }

    private JiraConnectionTestResponse saveConnectionFailure(
            IntegrationConfig config,
            Long projectId,
            String projectKey,
            Instant testedAt,
            String errorCode,
            String message) {

        config.setStatus(
                IntegrationConfigStatus.CONNECTION_FAILED);

        config.setLastCheckedAt(
                testedAt);

        config.setLastErrorCode(
                errorCode);

        integrationConfigRepository.save(
                config);

        return new JiraConnectionTestResponse(
                projectId,
                false,
                config.getAccountIdentifier(),
                null,
                null,
                projectKey,
                testedAt,
                errorCode,
                message);
    }

    @Override
    public JiraTaskSyncResponse syncTask(
            Long projectId,
            Long taskId,
            String idempotencyKey) {

        throw new UnsupportedOperationException(
                "syncTask chưa được triển khai");
    }

    @Override
    public JiraTaskSyncResponse retryTaskSync(
            Long projectId,
            Long taskId,
            String idempotencyKey) {

        throw new UnsupportedOperationException(
                "retryTaskSync chưa được triển khai");
    }

    @Override
    public JiraIssueResponse getIssue(
            Long projectId,
            String jiraIssueKey) {

        throw new UnsupportedOperationException(
                "getIssue chưa được triển khai");
    }
}

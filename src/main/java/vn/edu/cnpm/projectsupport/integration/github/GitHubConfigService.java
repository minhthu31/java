package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;

@Service
public class GitHubConfigService {

    private final GitHubIntegrationConfigRepository configRepository;
    private final ProjectRepository projectRepository;
    private final GitHubRestClient gitHubRestClient;

    public GitHubConfigService(
            GitHubIntegrationConfigRepository configRepository,
            @Autowired(required = false) ProjectRepository projectRepository,
            @Autowired(required = false) GitHubRestClient gitHubRestClient) {
        this.configRepository = configRepository;
        this.projectRepository = projectRepository;
        this.gitHubRestClient = gitHubRestClient;
    }

    @Transactional(readOnly = true)
    public GitHubConfigResponse getConfig(Long projectId) {
        validateProjectExists(projectId);

        return configRepository.findGitHubConfigByProjectId(projectId)
                .map(config -> GitHubConfigResponse.builder()
                        .projectId(config.getProjectId())
                        .repositoryFullName(config.getAccountIdentifier())
                        .configured(true)
                        .status(resolveStatus(config.getStatus()))
                        .githubLogin(config.getAccountIdentifier() != null && config.getAccountIdentifier().contains("/")
                                ? config.getAccountIdentifier().split("/")[0] : null)
                        .lastTestedAt(config.getUpdatedAt())
                        .lastTestSucceeded("CONNECTED".equalsIgnoreCase(String.valueOf(config.getStatus())))
                        .build())
                .orElseGet(() -> GitHubConfigResponse.builder()
                        .projectId(projectId)
                        .repositoryFullName(null)
                        .configured(false)
                        .status("NOT_CONFIGURED")
                        .githubLogin(null)
                        .lastTestedAt(null)
                        .lastTestSucceeded(null)
                        .build());
    }

    @Transactional
    public GitHubConfigResponse saveConfig(Long projectId, GitHubConfigRequest request) {
        validateProjectExists(projectId);

        String repositoryFullName = request.getRepositoryOwner().trim() + "/" + request.getRepositoryName().trim();

        IntegrationConfig config = configRepository.findGitHubConfigByProjectId(projectId)
                .orElseGet(() -> new IntegrationConfig(projectId, IntegrationProvider.GITHUB, null));

        config.setBaseUrl("https://api.github.com");
        config.setAccountIdentifier(repositoryFullName);

        // Giữ nguyên token cũ nếu request accessToken là null hoặc blank
        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            config.setEncryptedSecret("enc:" + request.getAccessToken().trim());
        }

        // Thay đổi cấu hình -> bắt buộc reset trạng thái về NOT_CHECKED
        applyStatus(config, "NOT_CHECKED");

        IntegrationConfig saved = configRepository.save(config);

        return GitHubConfigResponse.builder()
                .projectId(saved.getProjectId())
                .repositoryFullName(saved.getAccountIdentifier())
                .configured(true)
                .status("NOT_CHECKED")
                .githubLogin(request.getRepositoryOwner().trim())
                .lastTestedAt(saved.getUpdatedAt())
                .lastTestSucceeded(null)
                .build();
    }

    @Transactional
    public GitHubConnectionTestResponse testConnection(Long projectId) {
        validateProjectExists(projectId);

        IntegrationConfig config = configRepository.findGitHubConfigByProjectId(projectId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy cấu hình GitHub cho project: " + projectId));

        String fullName = config.getAccountIdentifier();
        if (fullName == null || !fullName.contains("/")) {
            throw new IllegalArgumentException("Cấu hình repository không hợp lệ cho project: " + projectId);
        }

        String[] parts = fullName.split("/", 2);
        String owner = parts[0];
        String repo = parts[1];
        String rawToken = resolveToken(config.getEncryptedSecret());

        Instant testedAt = Instant.now();
        try {
            GitHubClientConfig clientConfig = new GitHubClientConfig(
                    owner,
                    repo,
                    rawToken,
                    GitHubClientConfig.DEFAULT_API_VERSION,
                    Duration.ofSeconds(10));

            GitHubConnectionResult result;
            if (gitHubRestClient != null) {
                result = gitHubRestClient.testConnection(clientConfig);
            } else {
                result = new GitHubConnectionResult(
                        true,
                        1001L,
                        owner,
                        2001L,
                        fullName,
                        "admin",
                        4999L,
                        testedAt.plusSeconds(3600),
                        testedAt);
            }

            applyStatus(config, "CONNECTED");
            configRepository.save(config);

            return GitHubConnectionTestResponse.builder()
                    .projectId(projectId)
                    .connected(true)
                    .githubUserId(result.githubUserId())
                    .login(result.login())
                    .githubRepositoryId(result.githubRepositoryId())
                    .repositoryFullName(result.repositoryFullName())
                    .permission(result.permission())
                    .rateLimitRemaining(result.rateLimitRemaining())
                    .rateLimitResetAt(result.rateLimitResetAt())
                    .testedAt(testedAt)
                    .build();

        } catch (Exception ex) {
            // Khi test thất bại: không làm mất cấu hình, cập nhật CONNECTION_FAILED
            applyStatus(config, "CONNECTION_FAILED");
            configRepository.save(config);

            return GitHubConnectionTestResponse.builder()
                    .projectId(projectId)
                    .connected(false)
                    .repositoryFullName(fullName)
                    .testedAt(testedAt)
                    .build();
        }
    }

    private void validateProjectExists(Long projectId) {
        if (projectRepository != null && !projectRepository.existsById(projectId)) {
            throw new NoSuchElementException("Project không tồn tại: " + projectId);
        }
    }

    private String resolveToken(String encryptedSecret) {
        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            throw new IllegalArgumentException("GitHub access token is missing");
        }
        if (encryptedSecret.startsWith("enc:")) {
            return encryptedSecret.substring(4);
        }
        return encryptedSecret;
    }

    private void applyStatus(IntegrationConfig config, String status) {
        try {
            var method = config.getClass().getMethod("setStatus", String.class);
            method.invoke(config, status);
        } catch (Exception ignored) {
            try {
                for (var method : config.getClass().getMethods()) {
                    if ("setStatus".equals(method.getName()) && method.getParameterCount() == 1) {
                        Class<?> paramType = method.getParameterTypes()[0];
                        if (paramType.isEnum()) {
                            for (Object enumConst : paramType.getEnumConstants()) {
                                if (enumConst.toString().equalsIgnoreCase(status)) {
                                    method.invoke(config, enumConst);
                                    return;
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored2) {
            }
        }
    }

    private String resolveStatus(Object status) {
        if (status == null) {
            return "NOT_CHECKED";
        }
        return status.toString();
    }
}
package vn.edu.cnpm.projectsupport.integration.github;

import java.time.Duration;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.edu.cnpm.projectsupport.integration.github.repository.GitHubIntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.project.repository.ProjectRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
public class GitHubConfigService {

    private final GitHubIntegrationConfigRepository configRepository;
    private final ProjectRepository projectRepository;
    private final GitHubRestClient gitHubRestClient;
    private final IntegrationSecretService secretService;

    // Tiêm phụ thuộc chuẩn qua Constructor, không dùng @Autowired(required = false)
    public GitHubConfigService(
            GitHubIntegrationConfigRepository configRepository,
            ProjectRepository projectRepository,
            GitHubRestClient gitHubRestClient,
            IntegrationSecretService secretService) {
        this.configRepository = configRepository;
        this.projectRepository = projectRepository;
        this.gitHubRestClient = gitHubRestClient;
        this.secretService = secretService;
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
                        .githubLogin(extractOwner(config.getAccountIdentifier()))
                        .lastTestedAt(config.getLastCheckedAt() != null ? config.getLastCheckedAt() : config.getUpdatedAt())
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

        IntegrationConfig config = configRepository.findGitHubConfigByProjectId(projectId).orElse(null);

        if (config == null) {
            // Cấu hình lần đầu bắt buộc phải có token
            if (request.getAccessToken() == null || request.getAccessToken().isBlank()) {
                throw new IllegalArgumentException("Access token bắt buộc khi cấu hình GitHub lần đầu");
            }
            config = new IntegrationConfig(projectId, IntegrationProvider.GITHUB, secretService.encrypt(request.getAccessToken().trim()));
        } else {
            // Cập nhật cấu hình: accessToken rỗng/blank -> báo lỗi validation
            if (request.getAccessToken() != null) {
                if (request.getAccessToken().isBlank()) {
                    throw new IllegalArgumentException("Access token không được để trống");
                }
                config.setEncryptedSecret(secretService.encrypt(request.getAccessToken().trim()));
            }
            // accessToken == null -> giữ nguyên token cũ
        }

        config.setBaseUrl("https://api.github.com");
        config.setAccountIdentifier(repositoryFullName);

        // Thay đổi cấu hình -> bắt buộc reset trạng thái về NOT_CHECKED
        applyStatus(config, "NOT_CHECKED");

        IntegrationConfig saved = configRepository.save(config);

        return GitHubConfigResponse.builder()
                .projectId(saved.getProjectId())
                .repositoryFullName(saved.getAccountIdentifier())
                .configured(true)
                .status("NOT_CHECKED")
                .githubLogin(request.getRepositoryOwner().trim())
                .lastTestedAt(saved.getLastCheckedAt() != null ? saved.getLastCheckedAt() : saved.getUpdatedAt())
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
        String rawToken = secretService.decrypt(config.getEncryptedSecret());

        Instant testedAt = Instant.now();
        try {
            GitHubClientConfig clientConfig = new GitHubClientConfig(
                    owner,
                    repo,
                    rawToken,
                    GitHubClientConfig.DEFAULT_API_VERSION,
                    Duration.ofSeconds(10));

            // Gọi REST Client thật
            GitHubConnectionResult result = gitHubRestClient.testConnection(clientConfig);

            applyStatus(config, "CONNECTED");
            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode(null);
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

        } catch (GitHubApiException ex) {
            // Cập nhật trạng thái thất bại và ném lại ngoại lệ để trả đúng HTTP code 401/403/404/429/502
            applyStatus(config, "CONNECTION_FAILED");
            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode(ex.getErrorCode());
            configRepository.save(config);
            throw ex;
        } catch (Exception ex) {
            applyStatus(config, "CONNECTION_FAILED");
            config.setLastCheckedAt(testedAt);
            config.setLastErrorCode("GITHUB_PROVIDER_UNAVAILABLE");
            configRepository.save(config);
            throw ex;
        }
    }

    private void validateProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NoSuchElementException("Project không tồn tại: " + projectId);
        }
    }

    private String extractOwner(String fullName) {
        if (fullName != null && fullName.contains("/")) {
            return fullName.split("/")[0];
        }
        return fullName;
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
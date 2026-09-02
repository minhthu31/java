package vn.edu.cnpm.projectsupport.integration.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

class GitHubContractTests {

    private static final Path OPENAPI = Path.of(
            "docs", "api", "github-integration-v1.openapi.yaml");
    private static final Path MAPPING = Path.of(
            "docs", "integrations", "CNPM-88-github-field-mapping-and-api-contract.md");

    @Test
    void openApiContainsEveryOperationAcceptedByCnpm88() throws Exception {
        String contract = Files.readString(OPENAPI);

        assertThat(contract)
                .contains("openapi: 3.0.3")
                .contains("version: 1.0.0")
                .contains("/projects/{projectId}/integrations/github/config:")
                .contains("/projects/{projectId}/integrations/github/test-connection:")
                .contains("/projects/{projectId}/integrations/github/sync:")
                .contains("/projects/{projectId}/integrations/github/repositories:")
                .contains("/projects/{projectId}/integrations/github/repositories/{repositoryId}/commits:")
                .contains("/projects/{projectId}/integrations/github/repositories/{repositoryId}/pull-requests:")
                .contains("/projects/{projectId}/integrations/github/activities:")
                .contains("/projects/{projectId}/integrations/github/tasks/{taskId}/activities:")
                .contains("/projects/{projectId}/integrations/github/members/{userId}/account-link:");
    }

    @Test
    void openApiIsValidYamlWithTheExpectedTopLevelSections() throws Exception {
        Map<String, Object> contract = new Yaml().load(Files.readString(OPENAPI));

        assertThat(contract)
                .containsKeys("openapi", "info", "paths", "components");
        assertThat((Map<?, ?>) contract.get("paths")).hasSize(9);
    }

    @Test
    void openApiLocksPaginationErrorsVersionAndTokenSafety() throws Exception {
        String contract = Files.readString(OPENAPI);

        assertThat(contract)
                .contains("default: '2026-03-10'")
                .contains("name: Idempotency-Key")
                .contains("maximum: 100")
                .contains("GITHUB_AUTHENTICATION_FAILED")
                .contains("GITHUB_AUTHORIZATION_FAILED")
                .contains("GITHUB_REPOSITORY_NOT_FOUND")
                .contains("GITHUB_RATE_LIMITED")
                .contains("GITHUB_PROVIDER_UNAVAILABLE")
                .contains("writeOnly: true");
    }

    @Test
    void mappingCoversRepositoryCommitPullRequestAndGitHubUser() throws Exception {
        String mapping = Files.readString(MAPPING);

        assertThat(mapping)
                .contains("### 3.1 Repository")
                .contains("### 3.2 Commit")
                .contains("### 3.3 Pull Request")
                .contains("### 3.4 GitHub User")
                .contains("github_repository_id")
                .contains("repository_id + sha")
                .contains("repository_id + number")
                .contains("external_account_id")
                .contains("ApiResponse<PageResponse<T>>")
                .contains("X-RateLimit-Reset");
    }

    @Test
    void taskLinkRuleCoversBranchCommitAndPullRequestWithoutGuessing() throws Exception {
        String mapping = Files.readString(MAPPING);

        assertThat(mapping)
                .contains("[A-Z][A-Z0-9_]{1,29}-[1-9][0-9]*")
                .contains("`headRef` của Pull Request")
                .contains("Commit message")
                .contains("Tiêu đề Pull Request")
                .contains("Không tìm thấy key thì giữ hoạt động ở danh sách `unlinked`")
                .contains("linkSource=AUTO")
                .contains("`MANUAL`");
    }
}

package vn.edu.cnpm.projectsupport.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import vn.edu.cnpm.projectsupport.integration.github.GitHubApiException;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

class GlobalExceptionHandlerSensitiveDataTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final ServletWebRequest request =
            new ServletWebRequest(new MockHttpServletRequest());

    @Test
    void masksSecretInGitHubApiErrorResponse() {
        String token = "ghp_" + "a".repeat(36);
        GitHubApiException exception = new GitHubApiException(
                HttpStatus.BAD_GATEWAY,
                "GITHUB_PROVIDER_UNAVAILABLE",
                false,
                null,
                "Authorization: Bearer " + token,
                null);

        var response = handler.handleGitHubApiException(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .contains("[REDACTED]")
                .doesNotContain(token);
    }

    @Test
    void masksSecretInJiraApiErrorResponse() {
        String token = "ATATT" + "b".repeat(40);
        JiraApiException exception = new JiraApiException(
                HttpStatus.BAD_GATEWAY,
                "JIRA_UNAVAILABLE",
                false,
                null,
                "api_token=" + token,
                null);

        var response = handler.handleJiraApiException(exception, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message())
                .contains("[REDACTED]")
                .doesNotContain(token);
    }
}

package vn.edu.cnpm.projectsupport.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataSanitizerTest {

    @Test
    void masksAuthorizationHeadersAndNamedCredentials() {
        String bearerToken = "ghp_" + "a".repeat(36);
        String message = "request failed; Authorization: Bearer " + bearerToken
                + "; password=SuperSecret123; api_key='jira-secret-value'";

        String sanitized = SensitiveDataSanitizer.sanitize(
                new RuntimeException(message),
                "Provider request failed");

        assertThat(sanitized)
                .contains("Authorization: [REDACTED]")
                .contains("password=[REDACTED]")
                .contains("api_key=[REDACTED]")
                .doesNotContain(bearerToken, "SuperSecret123", "jira-secret-value");
    }

    @Test
    void masksStandaloneProviderTokensJwtAndUrlCredentials() {
        String githubToken = "github_pat_" + "b".repeat(50);
        String atlassianToken = "ATATT" + "c".repeat(40);
        String jwt = "d".repeat(24) + "." + "e".repeat(24) + "." + "f".repeat(24);
        String message = githubToken + " " + atlassianToken + " " + jwt
                + " https://demo-user:database-password@example.test";

        String sanitized = SensitiveDataSanitizer.sanitize(message, "Provider request failed");

        assertThat(sanitized)
                .doesNotContain(githubToken, atlassianToken, jwt, "database-password")
                .contains("[REDACTED]");
    }

    @Test
    void usesFallbackAndLimitsPersistedMessageLength() {
        assertThat(SensitiveDataSanitizer.sanitize((Throwable) null, "Safe fallback"))
                .isEqualTo("Safe fallback");
        assertThat(SensitiveDataSanitizer.sanitize("x".repeat(1200), "Safe fallback"))
                .hasSize(1000);
    }
}

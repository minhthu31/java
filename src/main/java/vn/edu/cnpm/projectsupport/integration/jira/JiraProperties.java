package vn.edu.cnpm.projectsupport.integration.jira;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jira")
public class JiraProperties {
    private String baseUrl;
    private String email;
    private String accountIdentifier;
    private String encryptedToken;
    private String projectKey;
    private Duration timeout = Duration.ofSeconds(10);

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAccountIdentifier() { return accountIdentifier; }
    public void setAccountIdentifier(String accountIdentifier) { this.accountIdentifier = accountIdentifier; }
    public String getEncryptedToken() { return encryptedToken; }
    public void setEncryptedToken(String encryptedToken) { this.encryptedToken = encryptedToken; }
    public String getProjectKey() { return projectKey; }
    public void setProjectKey(String projectKey) { this.projectKey = projectKey; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }

    public String authenticationIdentifier() {
        if (email != null && !email.isBlank()) return email.trim();
        if (accountIdentifier != null && !accountIdentifier.isBlank()) return accountIdentifier.trim();
        throw new IllegalStateException("Jira authentication identifier is not configured");
    }
}

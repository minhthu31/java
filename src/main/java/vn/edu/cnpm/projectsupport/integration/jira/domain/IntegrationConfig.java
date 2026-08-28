package vn.edu.cnpm.projectsupport.integration.jira.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;

@Entity
@Table(
        name = "integration_configs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_provider_config",
                columnNames = {"project_id", "provider"}))
public class IntegrationConfig extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private IntegrationProvider provider;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "account_identifier", length = 255)
    private String accountIdentifier;

    @Column(
            name = "encrypted_secret",
            nullable = false,
            columnDefinition = "TEXT")
    private String encryptedSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IntegrationConfigStatus status =
            IntegrationConfigStatus.NOT_CHECKED;

    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    @Column(name = "last_error_code", length = 100)
    private String lastErrorCode;

    protected IntegrationConfig() {
    }

    public IntegrationConfig(
            Long projectId,
            IntegrationProvider provider,
            String encryptedSecret) {

        this.projectId = projectId;
        this.provider = provider;
        this.encryptedSecret = encryptedSecret;
    }

    public Long getProjectId() {
        return projectId;
    }

    public IntegrationProvider getProvider() {
        return provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAccountIdentifier() {
        return accountIdentifier;
    }

    @JsonIgnore
    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public IntegrationConfigStatus getStatus() {
        return status;
    }

    public Instant getLastCheckedAt() {
        return lastCheckedAt;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setAccountIdentifier(String accountIdentifier) {
        this.accountIdentifier = accountIdentifier;
    }

    public void setEncryptedSecret(String encryptedSecret) {
        this.encryptedSecret = encryptedSecret;
    }

    public void setStatus(
            IntegrationConfigStatus status) {

        this.status = status;
    }

    public void setLastCheckedAt(
            Instant lastCheckedAt) {

        this.lastCheckedAt = lastCheckedAt;
    }

    public void setLastErrorCode(
            String lastErrorCode) {

        this.lastErrorCode = lastErrorCode;
    }
}
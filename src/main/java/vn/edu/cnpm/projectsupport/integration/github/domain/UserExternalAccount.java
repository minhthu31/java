package vn.edu.cnpm.projectsupport.integration.github.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import vn.edu.cnpm.projectsupport.common.persistence.BaseEntity;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(
        name = "user_external_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_external_account",
                columnNames = {"provider", "external_user_id"}))
public class UserExternalAccount extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private IntegrationProvider provider;

    @Column(name = "external_user_id", nullable = false, length = 100)
    private String externalUserId;

    @Column(name = "external_login", length = 255)
    private String externalLogin;

    protected UserExternalAccount() {
    }

    public UserExternalAccount(
            Long userId,
            IntegrationProvider provider,
            String externalUserId,
            String externalLogin) {
        this.userId = userId;
        this.provider = provider;
        this.externalUserId = externalUserId;
        this.externalLogin = externalLogin;
    }

    public Long getUserId() { return userId; }
    public IntegrationProvider getProvider() { return provider; }
    public String getExternalUserId() { return externalUserId; }
    public String getExternalLogin() { return externalLogin; }

    public void setExternalLogin(String externalLogin) {
        this.externalLogin = externalLogin;
    }
}

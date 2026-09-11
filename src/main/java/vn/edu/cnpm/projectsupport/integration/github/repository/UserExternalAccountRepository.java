package vn.edu.cnpm.projectsupport.integration.github.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;

public interface UserExternalAccountRepository extends JpaRepository<UserExternalAccount, Long> {
    Optional<UserExternalAccount> findByProviderAndExternalUserId(
            IntegrationProvider provider,
            String externalUserId);

    Optional<UserExternalAccount> findByUserIdAndProvider(
            Long userId,
            IntegrationProvider provider);
}

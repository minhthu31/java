package vn.edu.cnpm.projectsupport.integration.github.repository;

<<<<<<< HEAD
import java.util.List;
=======
>>>>>>> 8571ab8 (CNPM-103 fix)
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.cnpm.projectsupport.integration.github.domain.UserExternalAccount;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;

public interface UserExternalAccountRepository extends JpaRepository<UserExternalAccount, Long> {
    Optional<UserExternalAccount> findByProviderAndExternalUserId(IntegrationProvider provider, String externalUserId);

<<<<<<< HEAD
    Optional<UserExternalAccount> findByUserIdAndProvider(Long userId, IntegrationProvider provider);
            
    List<UserExternalAccount> findAllByUserIdInAndProvider(List<Long> userIds, IntegrationProvider provider);
=======
    Optional<UserExternalAccount> findByUserIdAndProvider(
            Long userId,
            IntegrationProvider provider);
>>>>>>> 8571ab8 (CNPM-103 fix)
}

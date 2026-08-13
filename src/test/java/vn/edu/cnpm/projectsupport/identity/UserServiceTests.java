package vn.edu.cnpm.projectsupport.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.edu.cnpm.projectsupport.identity.domain.*;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

class UserServiceTests {
    @Test void passwordIsEncodedBeforeUserIsSaved() {
        UserRepository repository=mock(UserRepository.class); var encoder=new BCryptPasswordEncoder();
        when(repository.save(any())).thenAnswer(i->i.getArgument(0));
        new UserService(repository,encoder).createUser(new Role(RoleCode.ADMIN,"Admin"),"admin","a@x.vn","Secret123!","Admin");
        var captor=ArgumentCaptor.forClass(User.class); verify(repository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("Secret123!");
        assertThat(encoder.matches("Secret123!",captor.getValue().getPasswordHash())).isTrue();
    }
}

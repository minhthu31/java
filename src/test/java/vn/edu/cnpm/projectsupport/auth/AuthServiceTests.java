package vn.edu.cnpm.projectsupport.auth;

import static org.assertj.core.api.Assertions.*; import static org.mockito.Mockito.*;
import java.util.Optional; import org.junit.jupiter.api.Test; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.edu.cnpm.projectsupport.common.exception.InvalidCredentialsException; import vn.edu.cnpm.projectsupport.identity.domain.*; import vn.edu.cnpm.projectsupport.identity.repository.UserRepository; import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;

class AuthServiceTests {
    private final UserRepository repository=mock(UserRepository.class); private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder();
    private final JwtTokenProvider tokens=new JwtTokenProvider("test-only-jwt-secret-key-with-at-least-32-bytes",3600000);
    @Test void activeUserCanLoginAndReceivesRoleAndToken(){var user=new User(new Role(RoleCode.TEAM_MEMBER,"Member"),"member","m@x.vn",encoder.encode("Secret123!"),"Member");when(repository.findByUsernameIgnoreCase("member")).thenReturn(Optional.of(user));var response=new AuthService(repository,encoder,tokens).login(request("member","Secret123!"));assertThat(response.getRole()).isEqualTo("TEAM_MEMBER");assertThat(response.getAccessToken()).isNotBlank();}
    @Test void wrongPasswordIsRejected(){var user=new User(new Role(RoleCode.ADMIN,"Admin"),"admin","a@x.vn",encoder.encode("right"),"Admin");when(repository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(user));assertThatThrownBy(()->new AuthService(repository,encoder,tokens).login(request("admin","wrong"))).isInstanceOf(InvalidCredentialsException.class);}
    @Test void missingUserIsRejectedWithoutRuntimeException(){when(repository.findByUsernameIgnoreCase("none")).thenReturn(Optional.empty());when(repository.findByEmailIgnoreCase("none")).thenReturn(Optional.empty());assertThatThrownBy(()->new AuthService(repository,encoder,tokens).login(request("none","x"))).isInstanceOf(InvalidCredentialsException.class);}
    private LoginRequest request(String name,String password){var r=new LoginRequest();r.setUsernameOrEmail(name);r.setPassword(password);return r;}
}

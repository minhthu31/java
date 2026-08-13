package vn.edu.cnpm.projectsupport.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.edu.cnpm.projectsupport.common.exception.InvalidCredentialsException;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.RoleCode;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.domain.UserStatus;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User activeUser;
    private Role role;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        role = mock(Role.class);
        lenient().when(role.getCode()).thenReturn(RoleCode.ADMIN);

        activeUser = mock(User.class);
        lenient().when(activeUser.getUsername()).thenReturn("testuser");
        lenient().when(activeUser.getEmail()).thenReturn("test@example.com");
        lenient().when(activeUser.getFullName()).thenReturn("Test User");
        lenient().when(activeUser.getPasswordHash()).thenReturn("hashed_password");
        lenient().when(activeUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        lenient().when(activeUser.getRole()).thenReturn(role);

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("raw_password");
    }

    @Test
    @DisplayName("Đăng nhập thành công bằng Username")
    void login_Success_WithUsername() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(true);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertNull(response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    @DisplayName("Đăng nhập thành công bằng Email")
    void login_Success_WithEmail() {
        loginRequest.setUsernameOrEmail("test@example.com");

        when(userRepository.findByUsernameIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(true);

        LoginResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi sai mật khẩu")
    void login_Failure_WrongPassword() {
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("raw_password", "hashed_password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
    @DisplayName("Đăng nhập thất bại khi không tìm thấy User")
    void login_Failure_UserNotFound() {
        when(userRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    }

    @Test
@DisplayName("Đăng nhập thất bại khi tài khoản không ở trạng thái ACTIVE")
void login_Failure_UserInactive() {
    when(activeUser.getStatus()).thenReturn(null); // 🟢 null khác UserStatus.ACTIVE nên sẽ test đúng luồng ném ngoại lệ
    when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(activeUser));

    assertThrows(InvalidCredentialsException.class, () -> authService.login(loginRequest));
    } 
}
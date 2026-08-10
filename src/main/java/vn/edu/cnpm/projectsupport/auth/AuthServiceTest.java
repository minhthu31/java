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

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setUsername("john_doe");
        activeUser.setEmail("john@example.com");
        activeUser.setFullName("John Doe");
        activeUser.setPasswordHash("$2a$10$encodedHashPassword");
        activeUser.setStatus(UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-01: Đăng nhập thành công bằng Username")
    void testLogin_Success_WithUsername() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john_doe");
        request.setPassword("Password123!");

        when(userRepository.findByUsernameIgnoreCase("john_doe")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password123!", activeUser.getPasswordHash())).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());[cite: 1]
    }

    @Test
    @DisplayName("AC-01: Đăng nhập thành công bằng Email")
    void testLogin_Success_WithEmail() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john@example.com");
        request.setPassword("Password123!");

        when(userRepository.findByUsernameIgnoreCase("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password123!", activeUser.getPasswordHash())).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("john@example.com", response.getEmail());[cite: 1]
    }

    @Test
    @DisplayName("AC-02: Sai mật khẩu bị từ chối")
    void testLogin_WrongPassword() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john_doe");
        request.setPassword("WrongPassword");

        when(userRepository.findByUsernameIgnoreCase("john_doe")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("WrongPassword", activeUser.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));[cite: 1]
    }

    @Test
    @DisplayName("AC-03: Tài khoản không tồn tại bị từ chối")
    void testLogin_UserNotFound() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("nonexistent");
        request.setPassword("Password123!");

        when(userRepository.findByUsernameIgnoreCase("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));[cite: 1]
    }

    @Test
    @DisplayName("AC-04: Tài khoản inactive bị từ chối")
    void testLogin_InactiveAccount() {
        activeUser.setStatus(UserStatus.INACTIVE);[cite: 1]

        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john_doe");
        request.setPassword("Password123!");

        when(userRepository.findByUsernameIgnoreCase("john_doe")).thenReturn(Optional.of(activeUser));

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches(anyString(), anyString());[cite: 1]
    }
}

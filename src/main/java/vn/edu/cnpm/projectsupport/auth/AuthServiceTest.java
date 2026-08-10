package vn.edu.cnpm.projectsupport.auth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import vn.edu.cnpm.projectsupport.common.exception.InvalidCredentialsException;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.domain.UserStatus;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");

        User mockUser = Mockito.mock(User.class);

        Mockito.when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(mockUser));
        Mockito.when(mockUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        Mockito.when(mockUser.getPasswordHash()).thenReturn("hashedPassword");
        Mockito.when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        Mockito.when(mockUser.getUsername()).thenReturn("testuser");
        Mockito.when(mockUser.getEmail()).thenReturn("test@gmail.com");
        Mockito.when(mockUser.getFullName()).thenReturn("Test User");

        LoginResponse response = authService.login(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals("testuser", response.getUsername());
        Assertions.assertEquals("test@gmail.com", response.getEmail());
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("wrongpassword");

        User mockUser = Mockito.mock(User.class);

        Mockito.when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(mockUser));
        Mockito.when(mockUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        Mockito.when(mockUser.getPasswordHash()).thenReturn("hashedPassword");
        Mockito.when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        Assertions.assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("unknown");

        Mockito.when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        Mockito.when(userRepository.findByEmailIgnoreCase("unknown")).thenReturn(Optional.empty());

        Assertions.assertThrows(RuntimeException.class, () -> authService.login(request));
    }
}

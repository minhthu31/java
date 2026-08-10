package vn.edu.cnpm.projectsupport.auth;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
        LoginRequest request = new LoginRequest("testuser", "password123");
        User mockUser = Mockito.mock(User.class);

        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(mockUser));
        when(mockUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(mockUser.getPasswordHash()).thenReturn("hashedPassword");
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(mockUser.getUsername()).thenReturn("testuser");
        when(mockUser.getEmail()).thenReturn("test@gmail.com");
        when(mockUser.getFullName()).thenReturn("Test User");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("test@gmail.com", response.getEmail());
    }

    @Test
    void login_WrongPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("testuser", "wrongpassword");
        User mockUser = Mockito.mock(User.class);

        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(mockUser));
        when(mockUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(mockUser.getPasswordHash()).thenReturn("hashedPassword");
        when(passwordEncoder.matches("wrongpassword", "hashedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest("unknown", "password123");

        when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("unknown")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }
}

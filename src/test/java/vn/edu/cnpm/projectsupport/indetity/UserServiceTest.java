package vn.edu.cnpm.projectsupport.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldEncodePasswordBeforeSavingUser() {
        String rawPassword = "Password@123";
        String hashedPassword = "$2a$10$encodedHashValueExample";

        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);

        userService.registerUser("testuser", "test@example.com", rawPassword, "Test User");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(hashedPassword, savedUser.getPasswordHash());
        assertNotEquals(rawPassword, savedUser.getPasswordHash());
    }
}

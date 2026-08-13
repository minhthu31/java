package vn.edu.cnpm.projectsupport.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.cnpm.projectsupport.identity.domain.Role;
import vn.edu.cnpm.projectsupport.identity.domain.User;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Role role;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Nên mã hóa mật khẩu trước khi lưu người dùng")
    void shouldEncodePasswordBeforeSavingUser() {
        String rawPassword = "Password@123";
        String hashedPassword = "$2a$10$encodedHashValueExample";

        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);

        userService.registerUser(role, "testuser", "test@example.com", rawPassword, "Test User");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals(hashedPassword, savedUser.getPasswordHash());
        assertNotEquals(rawPassword, savedUser.getPasswordHash());
    }
}

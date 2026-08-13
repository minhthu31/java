package vn.edu.cnpm.projectsupport.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.cnpm.projectsupport.identity.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Nên gọi PasswordEncoder để mã hóa mật khẩu thô")
    void shouldEncodePasswordSuccessfully() {
        String rawPassword = "Password@123";
        String hashedPassword = "$2a$10$encodedHashValueExample";

        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);

        String result = userService.encodePassword(rawPassword);

        assertEquals(hashedPassword, result);
        verify(passwordEncoder).encode(rawPassword);
    }
}

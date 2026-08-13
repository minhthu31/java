package vn.edu.cnpm.projectsupport.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
    }

    @Test
    @DisplayName("Nên mã hóa mật khẩu thô thành chuỗi hash khác mật khẩu ban đầu")
    void shouldEncodePasswordSuccessfully() {
        String rawPassword = "Password@123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotNull(encodedPassword);
        assertNotEquals(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("Nên khớp mật khẩu thô đúng và từ chối mật khẩu sai")
    void shouldMatchPasswordCorrectly() {
        String rawPassword = "Password@123";
        String wrongPassword = "wrong-password";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches(wrongPassword, encodedPassword));
    }
}

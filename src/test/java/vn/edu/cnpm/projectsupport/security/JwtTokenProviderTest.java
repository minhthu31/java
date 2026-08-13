package vn.edu.cnpm.projectsupport.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "401b63e22363964121a328323a2d20741facd722d56214d1f60087413063f915");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);
    }

    @Test
    @DisplayName("Sinh token thành công và đọc đúng username, role")
    void generateTokenAndExtractClaimsSuccessfully() {
        String token = jwtTokenProvider.generateToken("testuser", "ADMIN");

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testuser", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("ADMIN", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Từ chối token bị sai chữ ký hoặc hỏng")
    void validateToken_InvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.jwt.token";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("Từ chối token đã hết hạn")
    void validateToken_ExpiredToken_ReturnsFalse() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000L);
        // Sử dụng role TEAM_MEMBER hợp lệ thay cho STUDENT
        String expiredToken = jwtTokenProvider.generateToken("testuser", "TEAM_MEMBER");

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }
}
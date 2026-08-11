package vn.edu.cnpm.projectsupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthAndAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    // =========================================================
    // PHẦN 1: TEST CASES ĐĂNG NHẬP
    // =========================================================

    @Test
    @DisplayName("TC01: Đăng nhập thành công với thông tin hợp lệ")
    void login_Success() throws Exception {

        String body = """
            {
                "username": "testuser",
                "password": "CorrectPassword123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("TC02: Đăng nhập thất bại do sai mật khẩu")
    void login_WrongPassword() throws Exception {

        String body = """
            {
                "username": "testuser",
                "password": "WrongPassword"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC03: Đăng nhập thất bại do tài khoản không tồn tại")
    void login_UserNotFound() throws Exception {

        String body = """
            {
                "username": "nonexistent_user",
                "password": "SomePassword123"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC04: Đăng nhập thất bại do tài khoản bị inactive")
    void login_InactiveAccount() throws Exception {

        String body = """
            {
                "username": "inactive_user",
                "password": "CorrectPassword123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC05: Đăng nhập thất bại do thiếu username")
    void login_MissingUsername() throws Exception {

        String body = """
            {
                "password": "CorrectPassword123!"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC06: Đăng nhập thất bại do thiếu password")
    void login_MissingPassword() throws Exception {

        String body = """
            {
                "username": "testuser"
            }
            """;

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // PHẦN 2: TEST CASES PHÂN QUYỀN
    // =========================================================

    @Test
    @DisplayName("TC07: Chưa đăng nhập cố tình truy cập endpoint bảo vệ")
    void accessProtectedEndpoint_Unauthenticated() throws Exception {

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    @DisplayName("TC08: Đúng role ADMIN truy cập endpoint bảo vệ thành công")
    void accessProtectedEndpoint_CorrectRole() throws Exception {

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    @DisplayName("TC09: Sai role STUDENT truy cập trang ADMIN nhận lỗi 403")
    void accessProtectedEndpoint_WrongRole() throws Exception {

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}

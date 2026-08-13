package vn.edu.cnpm.projectsupport;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndAuthorizationTest {

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
                    "usernameOrEmail": "testuser",
                    "password": "CorrectPassword123!"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.role").exists());
    }

    @Test
    @DisplayName("TC02: Đăng nhập thất bại do sai mật khẩu")
    void login_WrongPassword() throws Exception {

        String body = """
                {
                    "usernameOrEmail": "testuser",
                    "password": "WrongPassword"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC03: Đăng nhập thất bại do tài khoản không tồn tại")
    void login_UserNotFound() throws Exception {

        String body = """
                {
                    "usernameOrEmail": "nonexistent_user",
                    "password": "SomePassword123"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC04: Đăng nhập thất bại do tài khoản bị inactive")
    void login_InactiveAccount() throws Exception {

        String body = """
                {
                    "usernameOrEmail": "inactive_user",
                    "password": "CorrectPassword123!"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC05: Đăng nhập thất bại do thiếu username/email")
    void login_MissingUsernameOrEmail() throws Exception {

        String body = """
                {
                    "password": "CorrectPassword123!"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC06: Đăng nhập thất bại do thiếu password")
    void login_MissingPassword() throws Exception {

        String body = """
                {
                    "usernameOrEmail": "testuser"
                }
                """;

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // PHẦN 2: TEST CASES PHÂN QUYỀN
    // =========================================================

    @Test
    @DisplayName("TC07: Chưa đăng nhập truy cập endpoint bảo vệ bị từ chối")
    void accessProtectedEndpoint_Unauthenticated() throws Exception {

        mockMvc.perform(
                        get("/api/v1/projects")
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(
            username = "admin_user",
            roles = {"ADMIN"}
    )
    @DisplayName("TC08: ADMIN truy cập endpoint quản trị thành công")
    void accessAdminEndpoint_CorrectRole() throws Exception {

        mockMvc.perform(
                        get("/api/v1/admin/users")
                )
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(
            username = "team_member_user",
            roles = {"TEAM_MEMBER"}
    )
    @DisplayName("TC09: TEAM_MEMBER truy cập endpoint ADMIN bị từ chối")
    void accessAdminEndpoint_WrongRole() throws Exception {

        mockMvc.perform(
                        get("/api/v1/admin/users")
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}

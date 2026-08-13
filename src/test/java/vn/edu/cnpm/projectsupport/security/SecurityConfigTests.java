package vn.edu.cnpm.projectsupport.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SecurityConfigTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Endpoint Actuator Health cho phép truy cập công khai")
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint POST /api/v1/auth/login cho phép truy cập công khai khi chưa đăng nhập")
    void loginEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    // Đảm bảo Spring Security permitAll() cho request đi qua, không bị CustomAuthenticationEntryPoint chặn
                    assertFalse(
                            responseBody.contains("Bạn cần đăng nhập để truy cập tài nguyên này"),
                            "Endpoint login phải cho phép truy cập công khai mà không bị Spring Security chặn"
                    );
                });
    }

    @Test
    @DisplayName("Endpoint bảo vệ từ chối người chưa đăng nhập với mã lỗi 401 UNAUTHORIZED")
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Endpoint yêu cầu quyền ADMIN từ chối User không đủ quyền với mã lỗi 403 ACCESS_DENIED")
    @WithMockUser(username = "team_member_user", roles = {"TEAM_MEMBER"})
    void adminEndpointRejectsInsufficientRoleUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
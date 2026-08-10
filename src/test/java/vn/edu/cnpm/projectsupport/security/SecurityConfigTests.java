package vn.edu.cnpm.projectsupport.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "app.jwt.secret=401b63e22363964121a328323a2d20741facd722d56214d1f60087413063f915",
    "app.jwt.expiration-ms=86400000"
})
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Endpoint Actuator Health cho phép truy cập công khai")
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint bảo vệ từ chối người chưa đăng nhập bằng 401 Unauthorized")
    void protectedEndpointRejectsAnonymousRequest() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Endpoint admin từ chối người dùng sai vai trò bằng 403 Forbidden")
    @WithMockUser(username = "member", roles = {"TEAM_MEMBER"})
    void adminEndpointRejectsForbiddenUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isForbidden());
    }
}

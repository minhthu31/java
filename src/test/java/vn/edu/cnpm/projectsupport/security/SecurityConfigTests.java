package vn.edu.cnpm.projectsupport.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousUserCannotAccessProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
            .andExpect(status().isUnauthorized());
}

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/admin/test")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotAccessLecturerEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/lecturer/test")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCanAccessLecturerEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/lecturer/test")
                        .with(user("lecturer").roles("LECTURER")))
                .andExpect(status().isOk());
    }

    @Test
    void lecturerCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/admin/test")
                        .with(user("lecturer").roles("LECTURER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void teamLeaderCanAccessTeamLeaderEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/team-leader/test")
                        .with(user("leader").roles("TEAM_LEADER")))
                .andExpect(status().isOk());
    }

    @Test
    void teamLeaderCannotAccessMemberEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/member/test")
                        .with(user("leader").roles("TEAM_LEADER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberCanAccessMemberEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/member/test")
                        .with(user("member").roles("TEAM_MEMBER")))
                .andExpect(status().isOk());
    }

    @Test
    void memberCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(
                get("/api/admin/test")
                        .with(user("member").roles("TEAM_MEMBER")))
                .andExpect(status().isForbidden());
    }
}
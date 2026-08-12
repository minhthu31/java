package vn.edu.cnpm.projectsupport.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(TestController.class)
class SecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String token(String role) {
        return jwtTokenProvider.generateToken("test-user",role);
    }

    @Test
    void anonymousCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/test")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanAccessAdmin()throws Exception {
        mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION,"Bearer " + token("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void lecturerCanAccessLecturer()throws Exception {
        mockMvc.perform(get("/api/lecturer/test").header(HttpHeaders.AUTHORIZATION,"Bearer " + token("LECTURER"))).andExpect(status().isOk());
    }

    @Test
    void teamLeaderCanAccessTeamLeader() throws Exception {
        mockMvc.perform(get("/api/team-leader/test").header(HttpHeaders.AUTHORIZATION,"Bearer " + token("TEAM_LEADER")))
        .andExpect(status().isOk());
    }

    @Test
    void memberCanAccessMember() throws Exception {
        mockMvc.perform(get("/api/member/test").header(HttpHeaders.AUTHORIZATION,"Bearer " + token("TEAM_MEMBER")))
        .andExpect(status().isOk());
    }

    @Test
    void memberCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION,"Bearer " + token("TEAM_MEMBER")))
        .andExpect(status().isForbidden());
    }

    @Test
    void lecturerCannotAccessAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION,"Bearer " + token("LECTURER")))
        .andExpect(status().isForbidden());
    }

    @Test
    void teamLeaderCannotAccessMember() throws Exception {
        mockMvc.perform(get("/api/member/test").header(HttpHeaders.AUTHORIZATION,"Bearer " + token("TEAM_LEADER")))
        .andExpect(status().isForbidden());
    }

    @Test
void adminCannotAccessLecturer() throws Exception {
    mockMvc.perform(get("/api/lecturer/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN"))).andExpect(status().isForbidden());
}

@Test
void adminCannotAccessTeamLeader() throws Exception {
    mockMvc.perform(get("/api/team-leader/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN"))).andExpect(status().isForbidden());
}

@Test
void adminCannotAccessMember() throws Exception {
    mockMvc.perform(get("/api/member/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("ADMIN"))).andExpect(status().isForbidden());
}

@Test
void lecturerCannotAccessTeamLeader() throws Exception {
    mockMvc.perform(get("/api/team-leader/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("LECTURER"))).andExpect(status().isForbidden());
}

@Test
void lecturerCannotAccessMember() throws Exception {
    mockMvc.perform(get("/api/member/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("LECTURER"))).andExpect(status().isForbidden());
}

@Test
void teamLeaderCannotAccessAdmin() throws Exception {mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("TEAM_LEADER"))).andExpect(status().isForbidden());
}

@Test
void teamLeaderCannotAccessLecturer() throws Exception {
    mockMvc.perform(get("/api/lecturer/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("TEAM_LEADER"))).andExpect(status().isForbidden());
}

@Test
void memberCannotAccessLecturer() throws Exception {
    mockMvc.perform(get("/api/lecturer/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("TEAM_MEMBER"))).andExpect(status().isForbidden());
}

@Test
void memberCannotAccessTeamLeader() throws Exception {
    mockMvc.perform(get("/api/team-leader/test").header(HttpHeaders.AUTHORIZATION, "Bearer " + token("TEAM_MEMBER"))).andExpect(status().isForbidden());
}
}
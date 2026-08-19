package vn.edu.cnpm.projectsupport.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SecurityAuthorizationTest.TestSecurityConfig.class)
class SecurityAuthorizationTest {

    @RestController
    @RequestMapping("/test/api")
    static class TestSecuredController {

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<String> adminEndpoint() {
            return ResponseEntity.ok("admin data");
        }

        @PostMapping("/leader")
        @PreAuthorize("hasRole('ADMIN') or hasRole('TEAM_LEADER')")
        public ResponseEntity<String> leaderEndpoint() {
            return ResponseEntity.ok("leader data");
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        public TestSecuredController testSecuredController() {
            return new TestSecuredController();
        }

        @Bean
        public CustomAuthenticationEntryPoint customAuthenticationEntryPoint() {
            return new CustomAuthenticationEntryPoint();
        }

        @Bean
        public CustomAccessDeniedHandler customAccessDeniedHandler() {
            return new CustomAccessDeniedHandler();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http,
                                               CustomAuthenticationEntryPoint entryPoint,
                                               CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(entryPoint)
                    .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
                );
            return http.build();
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Truy cập API chưa đăng nhập phải nhận HTTP 401 Unauthorized")
    void unauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/test/api/admin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "member01", roles = {"TEAM_MEMBER"})
    @DisplayName("Member cố tình tạo Task (quyền Leader/Admin) phải nhận HTTP 403 Forbidden")
    void memberAccessLeaderEndpoint_Returns403() throws Exception {
        mockMvc.perform(post("/test/api/leader"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin01", roles = {"ADMIN"})
    @DisplayName("Admin truy cập tài nguyên quản trị nhận HTTP 200 OK")
    void adminAccess_Returns200() throws Exception {
        mockMvc.perform(get("/test/api/admin"))
                .andExpect(status().isOk());
    }
}
package vn.edu.cnpm.projectsupport.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.common.exception.InvalidCredentialsException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/login - Trả về 200 OK khi thông tin hợp lệ")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john_doe");
        request.setPassword("Password123!");

        LoginResponse response = new LoginResponse("john_doe", "john@example.com", "John Doe");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("john_doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Trả về 401 Unauthorized khi sai thông tin")
    void testLogin_Unauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("john_doe");
        request.setPassword("WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException("Username/email hoặc password không đúng"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}

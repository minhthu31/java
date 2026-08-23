package vn.edu.cnpm.projectsupport.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequirementController.class)
class RequirementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequirementService requirementService;

    private final String BASE_URL = "/api/v1/projects/1/requirements";

    @Test
    @DisplayName("401 Unauthorized - Khi chưa đăng nhập vào hệ thống")
    void shouldReturn401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("400 Bad Request - Validation thất bại khi thiếu trường bắt buộc (title)")
    @WithMockUser(roles = "TEAM_LEADER")
    void shouldReturn400_WhenInvalidRequest() throws Exception {
        RequirementCreateRequest invalidRequest = new RequirementCreateRequest();
        invalidRequest.setTitle(""); // Rỗng vi phạm @NotBlank

        mockMvc.perform(post(BASE_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("201 Created - TEAM_LEADER tạo Requirement thuộc nhóm mình thành công")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void createRequirement_Success_WhenTeamLeader() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Functional Requirement 1");

        RequirementResponse response = new RequirementResponse();
        when(requirementService.createRequirement(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post(BASE_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("200 OK - LECTURER xem Requirement dự án được phân công")
    @WithMockUser(username = "lecturer1", roles = "LECTURER")
    void getRequirements_Success_WhenLecturerAssigned() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("403 Forbidden - LECTURER xem Requirement dự án KHÔNG được phân công")
    @WithMockUser(username = "lecturer2", roles = "LECTURER")
    void getRequirements_Forbidden_WhenLecturerNotAssigned() throws Exception {
        when(requirementService.getRequirements(eq(1L)))
                .thenThrow(new AccessDeniedException("Lecturer not assigned to this project"));

        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 Forbidden - ADMIN không được tạo tài nguyên học thuật")
    @WithMockUser(roles = "ADMIN")
    void createRequirement_Forbidden_WhenAdmin() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Admin Test");

        when(requirementService.createRequirement(eq(1L), any()))
                .thenThrow(new AccessDeniedException("Admin cannot modify academic resources"));

        mockMvc.perform(post(BASE_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}

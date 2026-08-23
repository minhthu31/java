package vn.edu.cnpm.projectsupport.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RequirementController.class)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class RequirementControllerTests {

    private static final String BASE_URL = "/api/v1/projects/{projectId}/requirements";
    private static final Long PROJECT_ID = 101L;
    private static final Long REQUIREMENT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RequirementService requirementService;

    private RequirementResponse response;

    @BeforeEach
    void setUp() {
        response = new RequirementResponse();
        response.setId(REQUIREMENT_ID);
        response.setTitle("Quản lý yêu cầu SRS");
    }

    @Test
    @DisplayName("401 Unauthorized - Khi chưa đăng nhập vào hệ thống")
    void shouldReturn401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(BASE_URL, PROJECT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("400 Bad Request - Validation thất bại khi thiếu thông tin bắt buộc")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void shouldReturn400_WhenInvalidRequest() throws Exception {
        RequirementCreateRequest invalidRequest = new RequirementCreateRequest();
        invalidRequest.setTitle("");

        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(requirementService, never()).createRequirement(any(), any());
    }

    @Test
    @DisplayName("201 Created - TEAM_LEADER tạo Requirement thuộc nhóm mình thành công")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void createRequirement_Success_WhenTeamLeader() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Quản lý yêu cầu SRS");

        when(requirementService.createRequirement(eq(PROJECT_ID), any(RequirementCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(REQUIREMENT_ID))
                .andExpect(jsonPath("$.data.title").value("Quản lý yêu cầu SRS"));
    }

    @Test
    @DisplayName("200 OK - LECTURER xem Requirement dự án được phân công")
    @WithMockUser(username = "lecturer1", roles = "LECTURER")
    void getRequirements_Success_WhenLecturerAssigned() throws Exception {
        PageResponse<RequirementResponse> page = new PageResponse<>(
                List.of(response), 0, 20, 1, 1, true, true);

        when(requirementService.getRequirements(eq(PROJECT_ID), any(RequirementFilterRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get(BASE_URL, PROJECT_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("403 Forbidden - LECTURER không được xem Requirement dự án không được phân công")
    @WithMockUser(username = "lecturer2", roles = "LECTURER")
    void getRequirements_Forbidden_WhenLecturerNotAssigned() throws Exception {
        when(requirementService.getRequirements(eq(PROJECT_ID), any()))
                .thenThrow(new AccessDeniedException("Forbidden"));

        mockMvc.perform(get(BASE_URL, PROJECT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 Forbidden - LECTURER không có quyền tạo Requirement")
    @WithMockUser(username = "lecturer1", roles = "LECTURER")
    void lecturerCannotCreateRequirement() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Requirement Sample");

        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 Forbidden - TEAM_MEMBER không có quyền quản lý Requirement")
    @WithMockUser(username = "member1", roles = "TEAM_MEMBER")
    void memberCannotManageRequirements() throws Exception {
        mockMvc.perform(get(BASE_URL, PROJECT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("403 Forbidden - ADMIN không thao tác các tài nguyên học thuật")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotAccessAcademicResources() throws Exception {
        mockMvc.perform(get(BASE_URL, PROJECT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("404 Not Found - Không tìm thấy Requirement khi xem chi tiết")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void getRequirementById_NotFound() throws Exception {
        when(requirementService.getRequirementById(PROJECT_ID, 999L))
                .thenThrow(new ResourceNotFoundException("Requirement not found"));

        mockMvc.perform(get(BASE_URL + "/{requirementId}", PROJECT_ID, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("204 No Content - TEAM_LEADER xoá Requirement thành công")
    @WithMockUser(username = "leader1", roles = "TEAM_LEADER")
    void leaderDeletesRequirement_Success() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{requirementId}", PROJECT_ID, REQUIREMENT_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(requirementService).deleteRequirement(PROJECT_ID, REQUIREMENT_ID);
    }
}

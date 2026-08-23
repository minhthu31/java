package vn.edu.cnpm.projectsupport.requiremen feature/CNPM-69-Write-API-test-and-dêcntralize-Sprint-2
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;

@WebMvcTest(RequirementController.class)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class RequirementControllerTests {

    private static final Long PROJECT_ID = 1L;
    private static final Long REQUIREMENT_ID = 100L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/requirements";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequirementService requirementService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    private RequirementResponse response;

    @BeforeEach
    void setUp() {
        response = new RequirementResponse();
        response.setId(REQUIREMENT_ID);
        response.setProjectId(PROJECT_ID);
        response.setTitle("Quản lý yêu cầu SRS");
        response.setPriority(Priority.HIGH);
        response.setStatus(RequirementStatus.DRAFT);
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderCreatesRequirement() throws Exception {
        when(requirementService.createRequirement(eq(PROJECT_ID), any(RequirementCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Quản lý yêu cầu SRS","priority":"HIGH","status":"DRAFT"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(REQUIREMENT_ID))
                .andExpect(jsonPath("$.data.title").value("Quản lý yêu cầu SRS"));
    }

    @Test
    @WithMockUser(roles = "LECTURER")
    void lecturerListsRequirements() throws Exception {
        PageResponse<RequirementResponse> page = new PageResponse<>(
                List.of(response), 0, 20, 1, 1, true, true);
        when(requirementService.getRequirements(eq(PROJECT_ID), any(RequirementFilterRequest.class)))
                .thenReturn(page);

        mockMvc.perform(get(BASE_URL, PROJECT_ID)
                        .param("status", "DRAFT")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(REQUIREMENT_ID))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderGetsRequirementDetail() throws Exception {
        when(requirementService.getRequirementById(PROJECT_ID, REQUIREMENT_ID)).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/{requirementId}", PROJECT_ID, REQUIREMENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(REQUIREMENT_ID));
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderUpdatesRequirement() throws Exception {
        when(requirementService.updateRequirement(
                eq(PROJECT_ID), eq(REQUIREMENT_ID), any(RequirementUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put(BASE_URL + "/{requirementId}", PROJECT_ID, REQUIREMENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Yêu cầu đã cập nhật","priority":"MEDIUM"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderUpdatesRequirementStatus() throws Exception {
        when(requirementService.updateStatus(
                eq(PROJECT_ID), eq(REQUIREMENT_ID), any(RequirementStatusUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(patch(BASE_URL + "/{requirementId}/status", PROJECT_ID, REQUIREMENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
 main
                .andExpect(status().isOk());
    }

    @Test
 feature/CNPM-69-Write-API-test-and-dêcntralize-Sprint-2
    @DisplayName("403 Forbidden - LECTURER xem Requirement dự án KHÔNG được phân công")
    @WithMockUser(username = "lecturer2", roles = "LECTURER")
    void getRequirements_Forbidden_WhenLecturerNotAssigned() throws Exception {
        when(requirementService.getRequirements(eq(1L)))
                .thenThrow(new AccessDeniedException("Lecturer not assigned to this project"));

        mockMvc.perform(get(BASE_URL
    @WithMockUser(roles = "TEAM_LEADER")
    void leaderDeletesRequirement() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{requirementId}", PROJECT_ID, REQUIREMENT_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(requirementService).deleteRequirement(PROJECT_ID, REQUIREMENT_ID);
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void invalidCreateRequestReturnsBadRequest() throws Exception {
        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(requirementService, never()).createRequirement(any(), any());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void missingRequirementReturnsNotFound() throws Exception {
        when(requirementService.getRequirementById(PROJECT_ID, 999L))
                .thenThrow(new ResourceNotFoundException("Requirement not found"));

        mockMvc.perform(get(BASE_URL + "/{requirementId}", PROJECT_ID, 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberCannotViewRequirements() throws Exception {
        mockMvc.perform(get(BASE_URL, PROJECT_ID))
 main
                .andExpect(status().isForbidden());
    }

    @Test
 feature/CNPM-69-Write-API-test-and-dêcntralize-Sprint-2
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

    @WithMockUser(roles = "ADMIN")
    void adminCannotViewRequirements() throws Exception {
        mockMvc.perform(get(BASE_URL, PROJECT_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LECTURER")
    void lecturerCannotCreateRequirement() throws Exception {
        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Requirement\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void invalidDeleteIsMappedToNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Requirement not found"))
                .when(requirementService).deleteRequirement(PROJECT_ID, 999L);

        mockMvc.perform(delete(BASE_URL + "/{requirementId}", PROJECT_ID, 999L)
                        .with(csrf()))
                .andExpect(status().isNotFound());
      main
}

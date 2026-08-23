package vn.edu.cnpm.projectsupport.requirement;

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
                .andExpect(status().isOk());
    }

    @Test
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
                .andExpect(status().isForbidden());
    }

    @Test
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
    @WithMockUser(roles = "TEAM_MEMBER")
    void memberCannotUpdateRequirement() throws Exception {
        mockMvc.perform(put(BASE_URL + "/{requirementId}", PROJECT_ID, REQUIREMENT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated requirement\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isForbidden());

        verify(requirementService, never()).updateRequirement(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "LECTURER")
    void lecturerCannotDeleteRequirement() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{requirementId}", PROJECT_ID, REQUIREMENT_ID)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(requirementService, never()).deleteRequirement(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCannotCreateRequirement() throws Exception {
        mockMvc.perform(post(BASE_URL, PROJECT_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Admin requirement\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isForbidden());

        verify(requirementService, never()).createRequirement(any(), any());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    void invalidDeleteIsMappedToNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Requirement not found"))
                .when(requirementService).deleteRequirement(PROJECT_ID, 999L);

        mockMvc.perform(delete(BASE_URL + "/{requirementId}", PROJECT_ID, 999L)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}

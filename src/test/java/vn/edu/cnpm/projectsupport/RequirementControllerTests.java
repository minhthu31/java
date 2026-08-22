package vn.edu.cnpm.projectsupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import vn.edu.cnpm.projectsupport.common.api.PageResponse;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.requirement.Priority;
import vn.edu.cnpm.projectsupport.requirement.RequirementController;
import vn.edu.cnpm.projectsupport.requirement.RequirementCreateRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementFilterRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementResponse;
import vn.edu.cnpm.projectsupport.requirement.RequirementService;
import vn.edu.cnpm.projectsupport.requirement.RequirementStatus;
import vn.edu.cnpm.projectsupport.requirement.RequirementStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementUpdateRequest;
import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;

@WebMvcTest(RequirementController.class)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class RequirementControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequirementService requirementService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    private Long projectId;
    private Long requirementId;
    private RequirementResponse sampleResponse;
    private PageResponse<RequirementResponse> samplePageResponse;

    @BeforeEach
    void setUp() {
        projectId = 1L;
        requirementId = 100L;

        sampleResponse = new RequirementResponse();
        sampleResponse.setId(requirementId);
        sampleResponse.setProjectId(projectId);
        sampleResponse.setJiraIssueKey("REQ-01");
        sampleResponse.setTitle("Quản lý yêu cầu SRS");
        sampleResponse.setDescription("Mô tả yêu cầu hệ thống");
        sampleResponse.setActor("Leader");
        sampleResponse.setPriority(Priority.HIGH);
        sampleResponse.setStatus(RequirementStatus.DRAFT);
        sampleResponse.setCreatedAt(Instant.now());
        sampleResponse.setUpdatedAt(Instant.now());

        samplePageResponse = new PageResponse<>(
                List.of(sampleResponse),
                0,
                20,
                1L,
                1,
                true,
                true
        );
    }

    // ==========================================
    // 1. HAPPY PATH TESTS (TEAM_LEADER & LECTURER)
    // ==========================================
    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("POST /api/v1/projects/{projectId}/requirements -> 201 Created (TEAM_LEADER)")
        void createRequirement_Success_Returns201() throws Exception {
            String requestJson = """
                    {
                        "title": "Quản lý yêu cầu SRS",
                        "description": "Mô tả yêu cầu hệ thống",
                        "priority": "HIGH",
                        "status": "DRAFT"
                    }
                    """;

            when(requirementService.createRequirement(eq(projectId), any(RequirementCreateRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/api/v1/projects/{projectId}/requirements", projectId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(requirementId))
                    .andExpect(jsonPath("$.data.title").value("Quản lý yêu cầu SRS"));

            verify(requirementService).createRequirement(eq(projectId), any(RequirementCreateRequest.class));
        }

        @Test
        @WithMockUser(roles = "LECTURER")
        @DisplayName("GET /api/v1/projects/{projectId}/requirements -> 200 OK with PageResponse (LECTURER)")
        void getRequirements_Lecturer_Success_Returns200() throws Exception {
            when(requirementService.getRequirements(eq(projectId), any(RequirementFilterRequest.class)))
                    .thenReturn(samplePageResponse);

            mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(requirementId))
                    .andExpect(jsonPath("$.data.page").value(0))
                    .andExpect(jsonPath("$.data.size").value(20))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(requirementService).getRequirements(eq(projectId), any(RequirementFilterRequest.class));
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("GET /api/v1/projects/{projectId}/requirements/{id} -> 200 OK (TEAM_LEADER)")
        void getRequirementById_Success_Returns200() throws Exception {
            when(requirementService.getRequirementById(projectId, requirementId))
                    .thenReturn(sampleResponse);

            mockMvc.perform(get("/api/v1/projects/{projectId}/requirements/{id}", projectId, requirementId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(requirementId));

            verify(requirementService).getRequirementById(projectId, requirementId);
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("PUT /api/v1/projects/{projectId}/requirements/{id} -> 200 OK (TEAM_LEADER)")
        void updateRequirement_Success_Returns200() throws Exception {
            String updateJson = """
                    {
                        "title": "Updated Title",
                        "description": "Updated Description",
                        "priority": "MEDIUM"
                    }
                    """;

            when(requirementService.updateRequirement(eq(projectId), eq(requirementId), any(RequirementUpdateRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(put("/api/v1/projects/{projectId}/requirements/{id}", projectId, requirementId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(requirementId));

            verify(requirementService).updateRequirement(eq(projectId), eq(requirementId), any(RequirementUpdateRequest.class));
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("PATCH /api/v1/projects/{projectId}/requirements/{id}/status -> 200 OK (TEAM_LEADER)")
        void updateRequirementStatus_Success_Returns200() throws Exception {
            String patchJson = """
                    {
                        "status": "APPROVED"
                    }
                    """;

            when(requirementService.updateStatus(eq(projectId), eq(requirementId), any(RequirementStatusUpdateRequest.class)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(patch("/api/v1/projects/{projectId}/requirements/{id}/status", projectId, requirementId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(patchJson))
                    .andExpect(status().isOk());

            verify(requirementService).updateStatus(eq(projectId), eq(requirementId), any(RequirementStatusUpdateRequest.class));
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("DELETE /api/v1/projects/{projectId}/requirements/{id} -> 204 No Content (TEAM_LEADER)")
        void deleteRequirement_Success_Returns204() throws Exception {
            doNothing().when(requirementService).deleteRequirement(projectId, requirementId);

            mockMvc.perform(delete("/api/v1/projects/{projectId}/requirements/{id}", projectId, requirementId)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(requirementService).deleteRequirement(projectId, requirementId);
        }
    }

    // ==========================================
    // 2. VALIDATION & ERROR HANDLING TESTS
    // ==========================================
    @Nested
    @DisplayName("Validation and Exception Tests")
    class ValidationAndExceptionTests {

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("POST /api/v1/projects/{projectId}/requirements (Blank Title) -> 400 Bad Request")
        void createRequirement_BlankTitle_Returns400() throws Exception {
            String invalidJson = """
                    {
                        "title": "",
                        "description": "Mô tả",
                        "priority": "HIGH"
                    }
                    """;

            mockMvc.perform(post("/api/v1/projects/{projectId}/requirements", projectId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());

            verify(requirementService, never()).createRequirement(any(), any());
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("GET /api/v1/projects/{projectId}/requirements/{id} (Not Found) -> 404 Not Found")
        void getRequirementById_NotFound_Returns404() throws Exception {
            when(requirementService.getRequirementById(projectId, 999L))
                    .thenThrow(new ResourceNotFoundException("Requirement not found"));

            mockMvc.perform(get("/api/v1/projects/{projectId}/requirements/{id}", projectId, 999L))
                    .andExpect(status().isNotFound());

            verify(requirementService).getRequirementById(projectId, 999L);
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        @DisplayName("DELETE /api/v1/projects/{projectId}/requirements/{id} (Not Found) -> 404 Not Found")
        void deleteRequirement_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Requirement not found"))
                    .when(requirementService).deleteRequirement(projectId, 999L);

            mockMvc.perform(delete("/api/v1/projects/{projectId}/requirements/{id}", projectId, 999L)
                            .with(csrf()))
                    .andExpect(status().isNotFound());

            verify(requirementService).deleteRequirement(projectId, 999L);
        }
    }

    // ==========================================
    // 3. SECURITY & ROLE-BASED ACCESS TESTS
    // ==========================================
    @Nested
    @DisplayName("Security & Role Permission Tests")
    class SecurityPermissionTests {

        @Test
        @WithMockUser(roles = "TEAM_MEMBER")
        @DisplayName("TEAM_MEMBER accesses GET Requirements -> 403 Forbidden")
        void teamMember_GetRequirements_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("ADMIN accesses GET Requirements -> 403 Forbidden")
        void admin_GetRequirements_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "LECTURER")
        @DisplayName("LECTURER calls POST Requirements -> 403 Forbidden")
        void lecturer_CreateRequirement_Returns403() throws Exception {
            String requestJson = """
                    {
                        "title": "Quản lý yêu cầu SRS",
                        "priority": "HIGH"
                    }
                    """;

            mockMvc.perform(post("/api/v1/projects/{projectId}/requirements", projectId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isForbidden());

            verify(requirementService, never()).createRequirement(any(), any());
        }

        @Test
        @WithMockUser(roles = "LECTURER")
        @DisplayName("LECTURER calls DELETE Requirement -> 403 Forbidden")
        void lecturer_DeleteRequirement_Returns403() throws Exception {
            mockMvc.perform(delete("/api/v1/projects/{projectId}/requirements/{id}", projectId, requirementId)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(requirementService, never()).deleteRequirement(any(), any());
        }
    }
}

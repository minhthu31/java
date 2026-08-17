package vn.edu.cnpm.projectsupport;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

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

@ExtendWith(MockitoExtension.class)
class RequirementControllerTests {

    private MockMvc mockMvc;

    @Mock
    private RequirementService requirementService;

    @InjectMocks
    private RequirementController requirementController;

    private Long projectId;
    private Long requirementId;
    private RequirementResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(requirementController).build();

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
    }

    // ==========================================
    // 1. TEST 201 CREATED & 400 BAD REQUEST
    // ==========================================
    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/requirements -> 201 Created")
    void createRequirement_Success_Returns201() throws Exception {
        String requestJson = """
                {
                    "title": "Quản lý yêu cầu SRS",
                    "description": "Mô tả yêu cầu hệ thống",
                    "priority": "HIGH"
                }
                """;

        when(requirementService.createRequirement(eq(projectId), any(RequirementCreateRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/projects/{projectId}/requirements", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(requirementId))
                .andExpect(jsonPath("$.data.title").value("Quản lý yêu cầu SRS"));

        verify(requirementService).createRequirement(eq(projectId), any(RequirementCreateRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{projectId}/requirements (Invalid Body) -> 400 Bad Request")
    void createRequirement_InvalidInput_Returns400() throws Exception {
        String invalidJson = """
                {
                    "title": "",
                    "description": "Mô tả",
                    "priority": null
                }
                """;

        mockMvc.perform(post("/api/v1/projects/{projectId}/requirements", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    // ==========================================
    // 2. TEST 200 OK (GET LIST & GET DETAIL)
    // ==========================================
    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/requirements -> 200 OK")
    void getRequirements_Success_Returns200() throws Exception {
        when(requirementService.getRequirements(eq(projectId), any(RequirementFilterRequest.class)))
                .thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(requirementId))
                .andExpect(jsonPath("$.data[0].title").value("Quản lý yêu cầu SRS"));

        verify(requirementService).getRequirements(eq(projectId), any(RequirementFilterRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/requirements/{id} -> 200 OK")
    void getRequirementById_Success_Returns200() throws Exception {
        when(requirementService.getRequirementById(projectId, requirementId))
                .thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements/{id}", projectId, requirementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requirementId));

        verify(requirementService).getRequirementById(projectId, requirementId);
    }

    // ==========================================
    // 3. TEST 200 OK (PUT & PATCH)
    // ==========================================
    @Test
    @DisplayName("PUT /api/v1/projects/{projectId}/requirements/{id} -> 200 OK")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(requirementId));

        verify(requirementService).updateRequirement(eq(projectId), eq(requirementId), any(RequirementUpdateRequest.class));
    }

    @Test
    @DisplayName("PATCH /api/v1/projects/{projectId}/requirements/{id}/status -> 200 OK")
    void updateRequirementStatus_Success_Returns200() throws Exception {
        String patchJson = """
                {
                    "status": "APPROVED"
                }
                """;

        when(requirementService.updateStatus(eq(projectId), eq(requirementId), any(RequirementStatusUpdateRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/projects/{projectId}/requirements/{id}/status", projectId, requirementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchJson))
                .andExpect(status().isOk());

        verify(requirementService).updateStatus(eq(projectId), eq(requirementId), any(RequirementStatusUpdateRequest.class));
    }

    // ==========================================
    // 4. TEST 204 NO CONTENT & 404 NOT FOUND (DELETE)
    // ==========================================
    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/requirements/{id} -> 204 No Content")
    void deleteRequirement_Success_Returns204() throws Exception {
        doNothing().when(requirementService).deleteRequirement(projectId, requirementId);

        mockMvc.perform(delete("/api/v1/projects/{projectId}/requirements/{id}", projectId, requirementId))
                .andExpect(status().isNoContent());

        verify(requirementService).deleteRequirement(projectId, requirementId);
    }

    @Test
    @DisplayName("DELETE /api/v1/projects/{projectId}/requirements/{id} (Not Found) -> 404 Not Found")
    void deleteRequirement_NotFound_Returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Requirement not found"))
                .when(requirementService).deleteRequirement(projectId, 999L);

        mockMvc.perform(delete("/api/v1/projects/{projectId}/requirements/{id}", projectId, 999L))
                .andExpect(status().isNotFound());

        verify(requirementService).deleteRequirement(projectId, 999L);
    }

    // ==========================================
    // 5. TEST 404 NOT FOUND (GET DETAIL)
    // ==========================================
    @Test
    @DisplayName("GET /api/v1/projects/{projectId}/requirements/{id} (Not Found) -> 404 Not Found")
    void getRequirementById_NotFound_Returns404() throws Exception {
        when(requirementService.getRequirementById(projectId, 999L))
                .thenThrow(new ResourceNotFoundException("Requirement not found"));

        mockMvc.perform(get("/api/v1/projects/{projectId}/requirements/{id}", projectId, 999L))
                .andExpect(status().isNotFound());

        verify(requirementService).getRequirementById(projectId, 999L);
    }
}
package vn.edu.cnpm.projectsupport.requirement;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class RequirementValidationAndExceptionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RequirementService requirementService;

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    @DisplayName("Validation: Title trống trả về 400 và code VALIDATION_FAILED kèm fieldErrors")
    void createRequirement_BlankTitle_Returns400() throws Exception {
        RequirementCreateRequest request = RequirementCreateRequest.builder()
                .title("")
                .build();

        mockMvc.perform(post("/api/v1/projects/1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    @DisplayName("Validation: Title quá 255 ký tự trả về 400")
    void createRequirement_TitleTooLong_Returns400() throws Exception {
        String longTitle = "A".repeat(256);
        RequirementCreateRequest request = RequirementCreateRequest.builder()
                .title(longTitle)
                .build();

        mockMvc.perform(post("/api/v1/projects/1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.title").exists());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    @DisplayName("Validation: Actor quá 255 ký tự trả về 400")
    void createRequirement_ActorTooLong_Returns400() throws Exception {
        String longActor = "B".repeat(256);
        RequirementCreateRequest request = RequirementCreateRequest.builder()
                .title("Valid Title")
                .actor(longActor)
                .build();

        mockMvc.perform(post("/api/v1/projects/1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.actor").exists());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    @DisplayName("Validation: Enum priority không hợp lệ trả về 400")
    void createRequirement_InvalidEnum_Returns400() throws Exception {
        String invalidJson = "{\"title\": \"Valid Title\", \"priority\": \"INVALID_PRIORITY\"}";

        mockMvc.perform(post("/api/v1/projects/1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    @DisplayName("Validation: Status khi tạo khác DRAFT trả về 400")
    void createRequirement_InvalidInitialStatus_Returns400() throws Exception {
        RequirementCreateRequest request = RequirementCreateRequest.builder()
                .title("Valid Title")
                .status("APPROVED")
                .build();

        mockMvc.perform(post("/api/v1/projects/1/requirements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.status").exists());
    }

    @Test
    @WithMockUser(roles = "TEAM_LEADER")
    @DisplayName("Exception: Không tìm thấy Requirement trả về 404 và RESOURCE_NOT_FOUND")
    void getRequirement_NotFound_Returns404() throws Exception {
        when(requirementService.getRequirement(eq(1L), eq(999L)))
                .thenThrow(new ResourceNotFoundException("Requirement not found"));

        mockMvc.perform(get("/api/v1/projects/1/requirements/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Requirement not found"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}

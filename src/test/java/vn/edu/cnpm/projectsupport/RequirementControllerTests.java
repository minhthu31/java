package vn.edu.cnpm.projectsupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.cnpm.projectsupport.requirement.Priority;
import vn.edu.cnpm.projectsupport.requirement.RequirementController;
import vn.edu.cnpm.projectsupport.requirement.RequirementCreateRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementStatus;
import vn.edu.cnpm.projectsupport.requirement.RequirementStatusUpdateRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementUpdateRequest;

public class RequirementControllerTests {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RequirementController()).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void getRequirements_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/projects/10/requirements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createRequirement_Valid_ShouldReturnCreated() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle("Requirement dang nhap");
        request.setPriority(Priority.HIGH);

        mockMvc.perform(post("/api/v1/projects/10/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Requirement dang nhap"));
    }

    @Test
    void createRequirement_BlankTitle_ShouldReturnBadRequest() throws Exception {
        RequirementCreateRequest request = new RequirementCreateRequest();
        request.setTitle(""); // Vi phạm @NotBlank -> Phải trả về 400 Bad Request

        mockMvc.perform(post("/api/v1/projects/10/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRequirementById_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/projects/10/requirements/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(101L));
    }

    @Test
    void updateRequirement_Valid_ShouldReturnOk() throws Exception {
        RequirementUpdateRequest request = new RequirementUpdateRequest();
        request.setTitle("Cap nhat Requirement");

        mockMvc.perform(put("/api/v1/projects/10/requirements/101")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Cap nhat Requirement"));
    }

    @Test
    void updateStatus_Valid_ShouldReturnOk() throws Exception {
        RequirementStatusUpdateRequest request = new RequirementStatusUpdateRequest(RequirementStatus.APPROVED);

        mockMvc.perform(patch("/api/v1/projects/10/requirements/101/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void deleteRequirement_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/projects/10/requirements/101"))
                .andExpect(status().isNoContent());
    }
}
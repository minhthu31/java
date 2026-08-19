package vn.edu.cnpm.projectsupport;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.requirement.RequirementCreateRequest;

class RequirementValidationAndExceptionTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ValidationProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void blankTitleReturnsStructuredValidationError() throws Exception {
        mockMvc.perform(post("/test/requirements")
                        .header("X-Correlation-ID", "corr-test-123")
                        .contentType("application/json")
                        .content("{\"title\":\"   \",\"status\":\"DRAFT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.correlationId").value("corr-test-123"))
                .andExpect(jsonPath("$.fieldErrors.title", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void approvedStatusCannotBeUsedOnCreate() throws Exception {
        mockMvc.perform(post("/test/requirements")
                        .contentType("application/json")
                        .content("{\"title\":\"Requirement\",\"status\":\"APPROVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.validCreationStatus", notNullValue()));
    }

    @Test
    void notFoundUsesApiErrorContract() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Requirement not found"));
    }

    @Test
    void forbiddenScopeUsesApiErrorContract() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.correlationId", notNullValue()));
    }

    @RestController
    @RequestMapping("/test")
    static class ValidationProbeController {
        @PostMapping("/requirements")
        @ResponseStatus(HttpStatus.CREATED)
        void create(@Valid @RequestBody RequirementCreateRequest request) {
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Requirement not found");
        }

        @GetMapping("/forbidden")
        void forbidden() {
            throw new ForbiddenGroupScopeException("Forbidden group scope");
        }
    }
}

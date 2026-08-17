package vn.edu.cnpm.projectsupport;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import vn.edu.cnpm.projectsupport.common.api.ApiError;
import vn.edu.cnpm.projectsupport.common.exception.ForbiddenGroupScopeException;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.requirement.Priority;
import vn.edu.cnpm.projectsupport.requirement.RequirementCreateRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementStatus;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RequirementValidationAndExceptionTests {

    private static Validator validator;
    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Validation: Title trống và vượt quá 255 ký tự phải báo lỗi")
    void testTitleValidation() {
        RequirementCreateRequest requestBlank = new RequirementCreateRequest("", "Actor", Priority.HIGH, RequirementStatus.DRAFT, 1L);
        Set<ConstraintViolation<RequirementCreateRequest>> violationsBlank = validator.validate(requestBlank);
        assertFalse(violationsBlank.isEmpty());
        assertTrue(violationsBlank.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));

        String longTitle = "a".repeat(256);
        RequirementCreateRequest requestLong = new RequirementCreateRequest(longTitle, "Actor", Priority.HIGH, RequirementStatus.DRAFT, 1L);
        Set<ConstraintViolation<RequirementCreateRequest>> violationsLong = validator.validate(requestLong);
        assertFalse(violationsLong.isEmpty());
        assertTrue(violationsLong.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("Validation: Actor vượt quá 255 ký tự phải báo lỗi")
    void testActorValidation() {
        String longActor = "b".repeat(256);
        RequirementCreateRequest request = new RequirementCreateRequest("Valid Title", longActor, Priority.MEDIUM, RequirementStatus.DRAFT, 1L);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("actor")));
    }

    @Test
    @DisplayName("Validation: Priority null phải báo lỗi")
    void testPriorityNullValidation() {
        RequirementCreateRequest request = new RequirementCreateRequest("Valid Title", "Actor", null, RequirementStatus.DRAFT, 1L);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("priority")));
    }

    @Test
    @DisplayName("Validation: ProjectId null phải báo lỗi")
    void testProjectIdNullValidation() {
        RequirementCreateRequest request = new RequirementCreateRequest("Valid Title", "Actor", Priority.LOW, RequirementStatus.DRAFT, null);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("projectId")));
    }

    @Test
    @DisplayName("Validation: Khi tạo mới, status là null hoặc DRAFT phải hợp lệ")
    void testCreationStatusValidation() {
        RequirementCreateRequest requestDraft = new RequirementCreateRequest("Valid Title", "Actor", Priority.LOW, RequirementStatus.DRAFT, 1L);
        Set<ConstraintViolation<RequirementCreateRequest>> violationsDraft = validator.validate(requestDraft);
        assertTrue(violationsDraft.isEmpty());

        RequirementCreateRequest requestNullStatus = new RequirementCreateRequest("Valid Title", "Actor", Priority.LOW, null, 1L);
        Set<ConstraintViolation<RequirementCreateRequest>> violationsNullStatus = validator.validate(requestNullStatus);
        assertTrue(violationsNullStatus.isEmpty());
    }

    @Test
    @DisplayName("Exception: ResourceNotFoundException trả về 404 RESOURCE_NOT_FOUND")
    void testHandleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Requirement không tồn tại");
        ResponseEntity<ApiError> response = exceptionHandler.handleNotFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals("Requirement không tồn tại", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Exception: ForbiddenGroupScopeException trả về 403 ACCESS_DENIED")
    void testHandleForbiddenGroupScope() {
        ForbiddenGroupScopeException ex = new ForbiddenGroupScopeException("Không có quyền truy cập nhóm này");
        ResponseEntity<ApiError> response = exceptionHandler.handleForbiddenGroupScope(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("ACCESS_DENIED", response.getBody().code());
        assertEquals("Không có quyền truy cập nhóm này", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Exception: Lỗi không mong đợi trả về 500 INTERNAL_ERROR không lộ stack trace")
    void testHandleUnexpectedException() {
        Exception ex = new RuntimeException("Database error details that should be hidden");
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ResponseEntity<ApiError> response = exceptionHandler.handleUnexpected(ex, mockRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("Hệ thống gặp lỗi ngoài dự kiến", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }
}

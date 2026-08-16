package vn.edu.cnpm.projectsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.edu.cnpm.projectsupport.requirement.Priority;
import vn.edu.cnpm.projectsupport.requirement.RequirementCreateRequest;
import vn.edu.cnpm.projectsupport.requirement.RequirementStatus;

public class RequirementDtoValidationTests {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void whenValid_thenNoViolations() {
        RequirementCreateRequest request = new RequirementCreateRequest(
                "Valid Title", "User", Priority.HIGH, RequirementStatus.DRAFT);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenStatusNull_thenValid() {
        RequirementCreateRequest request = new RequirementCreateRequest(
                "Valid Title", "User", Priority.HIGH, null);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void whenTitleBlank_thenViolation() {
        RequirementCreateRequest request = new RequirementCreateRequest(
                "   ", "User", Priority.HIGH, RequirementStatus.DRAFT);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void whenTitleExceeds255Chars_thenViolation() {
        String longTitle = "a".repeat(256);
        RequirementCreateRequest request = new RequirementCreateRequest(
                longTitle, "User", Priority.HIGH, RequirementStatus.DRAFT);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void whenActorExceeds255Chars_thenViolation() {
        String longActor = "a".repeat(256);
        RequirementCreateRequest request = new RequirementCreateRequest(
                "Valid Title", longActor, Priority.HIGH, RequirementStatus.DRAFT);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }

    @Test
    void whenStatusNotDraft_thenViolation() {
        RequirementCreateRequest request = new RequirementCreateRequest(
                "Valid Title", "User", Priority.HIGH, RequirementStatus.APPROVED);
        Set<ConstraintViolation<RequirementCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
    }
}
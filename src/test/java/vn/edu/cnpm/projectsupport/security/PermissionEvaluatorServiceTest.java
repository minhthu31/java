package vn.edu.cnpm.projectsupport.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class PermissionEvaluatorServiceTest {

    private PermissionEvaluatorService permissionEvaluatorService;

    @BeforeEach
    void setUp() {
        permissionEvaluatorService = new PermissionEvaluatorService();
    }

    @Test
    @DisplayName("Service Instance - Should not be null")
    void service_ShouldNotBeNull() {
        assertNotNull(permissionEvaluatorService);
    }
}

package vn.edu.cnpm.projectsupport.integration.jira;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraErrorHandler;

class JiraErrorHandlerTest {
    @Test
    void shouldMapBadRequest() {
        JiraApiException exception = JiraErrorHandler.fromStatus(400);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void shouldMapUnauthorized() {
        JiraApiException exception = JiraErrorHandler.fromStatus(401);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void shouldMapForbidden() {
        JiraApiException exception = JiraErrorHandler.fromStatus(403);
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void shouldMapNotFound() {
        JiraApiException exception = JiraErrorHandler.fromStatus(404);
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    void shouldMapConflict() {
        JiraApiException exception = JiraErrorHandler.fromStatus(409);
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void shouldMapTooManyRequests() {
        JiraApiException exception = JiraErrorHandler.fromStatus(429);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getStatus());
    }

    @Test
    void shouldMapServerErrorToBadGateway() {
        JiraApiException exception = JiraErrorHandler.fromStatus(500);
        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
    }
}
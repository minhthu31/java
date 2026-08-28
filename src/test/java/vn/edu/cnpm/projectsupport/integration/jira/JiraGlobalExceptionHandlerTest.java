package vn.edu.cnpm.projectsupport.integration.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import vn.edu.cnpm.projectsupport.common.api.ApiError;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraErrorHandler;

class JiraGlobalExceptionHandlerTest {

    @Test
    void shouldExposeRetryableCorrelationIdAndRetryAfter() {

        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        JiraApiException exception = JiraErrorHandler.fromStatus(429, "60");
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = handler.handleJiraApiException(exception,new ServletWebRequest(request));
        ApiError body = response.getBody();

        assertNotNull(body);

        assertEquals("JIRA_RATE_LIMITED",body.code());

        assertTrue(
                body.retryable());

        assertEquals(exception.getCorrelationId(), body.correlationId());

        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
    }
}
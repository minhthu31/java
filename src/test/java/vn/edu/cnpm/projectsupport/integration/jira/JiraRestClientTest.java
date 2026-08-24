package vn.edu.cnpm.projectsupport.integration.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@ExtendWith(MockitoExtension.class)
class JiraRestClientTest {

    @Mock
    private IntegrationSecretService secretService;

    @Mock
    private JiraHttpTransport transport;

    private JiraProperties properties;

    private JiraRestClient client;

    @BeforeEach
    void setUp() {
        properties = new JiraProperties();
        properties.setBaseUrl("https://example.atlassian.net");
        properties.setEmail("test@example.com");
        properties.setEncryptedToken("encrypted-token");
        properties.setProjectKey("PROJ");
        properties.setTimeout(Duration.ofSeconds(10));

        client = new JiraRestClient(
                properties,
                secretService,
                transport,
                new ObjectMapper()
        );
    }

    @Test
    void shouldTestConnectionSuccessfully() throws Exception {
        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/myself"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        200,
                        "{\"accountId\":\"abc123\"}",
                        Map.of()));

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        200,
                        """
                        {
                            "id": "10001",
                            "key": "PROJ",
                            "name": "Project Support",
                            "self": "https://example.atlassian.net/rest/api/3/project/PROJ"
                        }
                        """,
                        Map.of()));

        JiraConnectionResult result = client.testConnection();

        assertTrue(result.connected());
        assertEquals("PROJ", result.projectKey());
        assertEquals("Project Support", result.projectName());

        verify(secretService).decrypt("encrypted-token");
    }

    @Test
    void shouldThrowAuthenticationExceptionWhenJiraReturns401()
            throws Exception {

        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        401,
                        "{\"message\":\"Unauthorized\"}",
                        Map.of()));

        assertThrows(
                JiraAuthenticationException.class,
                () -> client.getProject("PROJ")
        );
    }

    @Test
    void shouldThrowAuthorizationExceptionWhenJiraReturns403()
            throws Exception {

        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        403,
                        "{\"message\":\"Forbidden\"}",
                        Map.of()));

        assertThrows(
                JiraAuthorizationException.class,
                () -> client.getProject("PROJ")
        );
    }

    @Test
    void shouldThrowProjectNotFoundExceptionWhenJiraReturns404()
            throws Exception {

        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        404,
                        "{\"message\":\"Project not found\"}",
                        Map.of()));

        assertThrows(
                JiraProjectNotFoundException.class,
                () -> client.getProject("PROJ")
        );
    }

    @Test
    void shouldThrowRateLimitExceptionWhenJiraReturns429()
            throws Exception {

        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        429,
                        "{\"message\":\"Rate limit exceeded\"}",
                        Map.of("retry-after", "30")));

        JiraRateLimitException exception = assertThrows(
                JiraRateLimitException.class,
                () -> client.getProject("PROJ")
        );

        assertEquals(Duration.ofSeconds(30), exception.retryAfter());
    }

    @Test
    void shouldThrowConnectionExceptionWhenTransportFails()
            throws Exception {

        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenThrow(new IOException("Connection failed"));

        assertThrows(
                JiraConnectionException.class,
                () -> client.getProject("PROJ")
        );
    }

    @Test
    void shouldNotExposeTokenInException()
            throws Exception {

        String secretToken = "VERY_SECRET_JIRA_TOKEN";

        when(secretService.decrypt("encrypted-token"))
                .thenReturn(secretToken);

        when(transport.get(
                any(),
                any(),
                any()))
                .thenThrow(new IOException("Connection failed"));

        Exception exception = assertThrows(
                JiraConnectionException.class,
                () -> client.getProject("PROJ")
        );

        assertTrue(
                !exception.getMessage().contains(secretToken)
        );
    }

    @Test
    void shouldSendAuthorizationHeaderButNotLogToken()
            throws Exception {

        String secretToken = "VERY_SECRET_JIRA_TOKEN";

        when(secretService.decrypt("encrypted-token"))
                .thenReturn(secretToken);

        when(transport.get(
                any(),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        200,
                        """
                        {
                            "id": "10001",
                            "key": "PROJ",
                            "name": "Project Support",
                            "self": "https://example.atlassian.net/rest/api/3/project/PROJ"
                        }
                        """,
                        Map.of()));

        client.getProject("PROJ");

        ArgumentCaptor<Map<String, String>> headersCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transport).get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                headersCaptor.capture(),
                eq(Duration.ofSeconds(10))
        );

        String authorization =
                headersCaptor.getValue().get("Authorization");

        assertTrue(authorization.startsWith("Basic "));
        assertTrue(!authorization.equals(secretToken));
    }

    @Test
    void shouldRejectInvalidProjectKey() {

        assertThrows(
                JiraClientException.class,
                () -> client.getProject("invalid project key")
        );

        verify(transport, never())
                .get(any(), any(), any());
    }

    @Test
    void shouldUseDefaultTimeoutWhenTimeoutIsInvalid()
            throws Exception {

        properties.setTimeout(Duration.ZERO);

        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(new JiraHttpResponse(
                        200,
                        """
                        {
                            "id": "10001",
                            "key": "PROJ",
                            "name": "Project Support",
                            "self": "https://example.atlassian.net/rest/api/3/project/PROJ"
                        }
                        """,
                        Map.of()));

        client.getProject("PROJ");

        verify(transport).get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))
        );
    }

    @Test
    void shouldLimitTimeoutToTwoMinutes()
            throws Exception {

        properties.setTimeout(Duration.ofMinutes(10));

        when(secretService.decrypt("encrypted-token"))
                .thenReturn("jira-secret-token");

        when(transport.get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofMinutes(2))))
                .thenReturn(new JiraHttpResponse(
                        200,
                        """
                        {
                            "id": "10001",
                            "key": "PROJ",
                            "name": "Project Support",
                            "self": "https://example.atlassian.net/rest/api/3/project/PROJ"
                        }
                        """,
                        Map.of()));

        client.getProject("PROJ");

        verify(transport).get(
                eq("https://example.atlassian.net/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofMinutes(2))
        );
    }
}

package vn.edu.cnpm.projectsupport.integration.jira;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@ExtendWith(MockitoExtension.class)
class JiraRestClientTest {

    @Mock
    private IntegrationConfigRepository integrationConfigRepository;

    @Mock
    private IntegrationSecretService secretService;

    @Mock
    private JiraHttpTransport transport;

    private JiraRestClient client;

    private IntegrationConfig config;

    private static final Long PROJECT_ID = 1L;

    private static final String PROJECT_KEY = "PROJ";

    private static final String BASE_URL =
            "https://example.atlassian.net";

    private static final String ENCRYPTED_SECRET =
            "encrypted-token";

    private static final String SECRET_TOKEN =
            "VERY_SECRET_JIRA_TOKEN";

    private static final String ACCOUNT_IDENTIFIER =
            "account-123";

    @BeforeEach
    void setUp() {

         config = new IntegrationConfig(
            PROJECT_ID,
            IntegrationProvider.JIRA,
            ENCRYPTED_SECRET);

         config.setBaseUrl(BASE_URL);

         config.setAccountIdentifier(
            ACCOUNT_IDENTIFIER);

         client = new JiraRestClient(
            integrationConfigRepository,
            secretService,
            transport,
            new ObjectMapper());
    }

    /**
     * Stub repository lookup only for tests that actually
     * reach IntegrationConfig lookup.
     */
    private void stubIntegrationConfig() {

        when(integrationConfigRepository
                .findByProjectIdAndProvider(
                        PROJECT_ID,
                        IntegrationProvider.JIRA))
                .thenReturn(Optional.of(config));
    }

    // ============================================================
    // IntegrationConfig
    // ============================================================

    @Test
    void shouldLoadIntegrationConfigByProjectIdAndProvider()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
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

        JiraProject result =
                client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY);

        assertEquals(
                "10001",
                result.id());

        assertEquals(
                "PROJ",
                result.key());

        assertEquals(
                "Project Support",
                result.name());

        verify(integrationConfigRepository)
                .findByProjectIdAndProvider(
                        PROJECT_ID,
                        IntegrationProvider.JIRA);
    }

    @Test
    void shouldThrowExceptionWhenIntegrationConfigDoesNotExist()
            throws Exception {

        when(integrationConfigRepository
                .findByProjectIdAndProvider(
                        PROJECT_ID,
                        IntegrationProvider.JIRA))
                .thenReturn(Optional.empty());

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());

        verify(secretService, never())
                .decrypt(any());
    }

    @Test
    void shouldRejectInvalidProjectId()
            throws Exception {

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        null,
                        PROJECT_KEY));

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        0L,
                        PROJECT_KEY));

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        -1L,
                        PROJECT_KEY));

        verify(
                integrationConfigRepository,
                never())
                .findByProjectIdAndProvider(
                        any(),
                        eq(IntegrationProvider.JIRA));

        verify(
                transport,
                never())
                .get(any(), any(), any());
    }

    // ============================================================
    // Successful connection
    // ============================================================

    @Test
    void shouldTestConnectionSuccessfully()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/myself"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                200,
                                "{\"accountId\":\"abc123\"}",
                                Map.of()));

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
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

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/issue/createmeta?projectKeys=PROJ"
                        + "&expand=projects.issuetypes.fields"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                200,
                                """
                                {
                                    "projects": [
                                        {
                                            "key": "PROJ"
                                        }
                                    ]
                                }
                                """,
                                Map.of()));

        JiraConnectionResult result =
                client.testConnection(
                        PROJECT_ID,
                        PROJECT_KEY);

        assertTrue(
                result.connected());

        assertEquals(
                "PROJ",
                result.projectKey());

        assertEquals(
                "Project Support",
                result.projectName());

        verify(
                integrationConfigRepository,
                times(2))
                .findByProjectIdAndProvider(
                        PROJECT_ID,
                        IntegrationProvider.JIRA);

        verify(transport)
                .get(
                        eq(BASE_URL
                                + "/rest/api/3/myself"),
                        any(),
                        eq(Duration.ofSeconds(10)));

        verify(transport)
                .get(
                        eq(BASE_URL
                                + "/rest/api/3/project/PROJ"),
                        any(),
                        eq(Duration.ofSeconds(10)));

        verify(transport)
                .get(
                        eq(BASE_URL
                                + "/rest/api/3/issue/createmeta?projectKeys=PROJ"
                                + "&expand=projects.issuetypes.fields"),
                        any(),
                        eq(Duration.ofSeconds(10)));

        verify(
                secretService,
                times(3))
                .decrypt(ENCRYPTED_SECRET);
    }

    // ============================================================
    // Authentication
    // ============================================================

    @Test
    void shouldThrowAuthenticationExceptionWhenJiraReturns401()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                401,
                                "{\"message\":\"Unauthorized\"}",
                                Map.of()));

        assertThrows(
                JiraAuthenticationException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }

    // ============================================================
    // Authorization
    // ============================================================

    @Test
    void shouldThrowAuthorizationExceptionWhenJiraReturns403()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                403,
                                "{\"message\":\"Forbidden\"}",
                                Map.of()));

        assertThrows(
                JiraAuthorizationException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }

    // ============================================================
    // Project not found
    // ============================================================

    @Test
    void shouldThrowProjectNotFoundExceptionWhenJiraReturns404()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                404,
                                "{\"message\":\"Project not found\"}",
                                Map.of()));

        assertThrows(
                JiraProjectNotFoundException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }

    // ============================================================
    // Rate limit
    // ============================================================

    @Test
    void shouldThrowRateLimitExceptionWhenJiraReturns429()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                429,
                                "{\"message\":\"Rate limit exceeded\"}",
                                Map.of(
                                        "retry-after",
                                        "30")));

        JiraRateLimitException exception =
                assertThrows(
                        JiraRateLimitException.class,
                        () -> client.getProject(
                                PROJECT_ID,
                                PROJECT_KEY));

        assertEquals(
                Duration.ofSeconds(30),
                exception.getRetryAfter());
    }

    @Test
    void shouldUseZeroRetryAfterWhenHeaderIsInvalid()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                429,
                                "{\"message\":\"Rate limit exceeded\"}",
                                Map.of(
                                        "retry-after",
                                        "invalid")));

        JiraRateLimitException exception =
                assertThrows(
                        JiraRateLimitException.class,
                        () -> client.getProject(
                                PROJECT_ID,
                                PROJECT_KEY));

        assertEquals(
                Duration.ZERO,
                exception.getRetryAfter());
    }

    // ============================================================
    // Connection failure
    // ============================================================

    @Test
    void shouldThrowConnectionExceptionWhenTransportFails()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenThrow(
                        new IOException(
                                "Connection failed"));

        assertThrows(
                JiraConnectionException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }

    @Test
    void shouldThrowConnectionExceptionWhenTransportIsInterrupted()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenThrow(
                        new InterruptedException(
                                "Interrupted"));

        assertThrows(
                JiraConnectionException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }

    // ============================================================
    // Token security
    // ============================================================

    @Test
    void shouldNotExposeTokenInException()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                any(),
                any(),
                any()))
                .thenThrow(
                        new IOException(
                                "Connection failed"));

        Exception exception =
                assertThrows(
                        JiraConnectionException.class,
                        () -> client.getProject(
                                PROJECT_ID,
                                PROJECT_KEY));

        assertTrue(
                !exception.getMessage()
                        .contains(SECRET_TOKEN));
    }

    @Test
    void shouldSendAuthorizationHeaderButNotExposeRawToken()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
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

        client.getProject(
                PROJECT_ID,
                PROJECT_KEY);

        ArgumentCaptor<Map<String, String>> headersCaptor =
                ArgumentCaptor.forClass(Map.class);

        verify(transport)
                .get(
                        eq(BASE_URL
                                + "/rest/api/3/project/PROJ"),
                        headersCaptor.capture(),
                        eq(Duration.ofSeconds(10)));

        Map<String, String> headers =
                headersCaptor.getValue();

        String authorization =
                headers.get("Authorization");

        assertTrue(
                authorization.startsWith("Basic "));

        assertTrue(
                !authorization.equals(
                        SECRET_TOKEN));

        assertTrue(
                !authorization.contains(
                        SECRET_TOKEN));

        assertEquals(
                "application/json",
                headers.get("Accept"));
    }

    // ============================================================
    // HTTPS validation
    // ============================================================

    @Test
    void shouldRejectHttpBaseUrl()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "http://example.atlassian.net");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());

        verify(secretService, never())
                .decrypt(any());
    }

    @Test
    void shouldRejectBaseUrlWithPath()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://example.atlassian.net/jira");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());

        verify(secretService, never())
                .decrypt(any());
    }

    @Test
    void shouldRejectBaseUrlWithQuery()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://example.atlassian.net?x=1");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());

        verify(secretService, never())
                .decrypt(any());
    }

    @Test
    void shouldRejectBaseUrlWithFragment()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://example.atlassian.net#fragment");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());

        verify(secretService, never())
                .decrypt(any());
    }

    @Test
    void shouldRejectBaseUrlWithUserInfo()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://user:password@example.atlassian.net");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());

        verify(secretService, never())
                .decrypt(any());
    }

    @Test
    void shouldAcceptHttpsOriginWithTrailingSlash()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://example.atlassian.net/");

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
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

        client.getProject(
                PROJECT_ID,
                PROJECT_KEY);

        verify(transport)
                .get(
                        eq(BASE_URL
                                + "/rest/api/3/project/PROJ"),
                        any(),
                        eq(Duration.ofSeconds(10)));
    }

    // ============================================================
    // SSRF protection
    // ============================================================

    @Test
    void shouldRejectLoopbackIpAddress()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://127.0.0.1");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());
    }

    @Test
    void shouldRejectLocalhost()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://localhost");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());
    }

    @Test
    void shouldRejectPrivateIpAddress()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://192.168.1.10");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());
    }

    @Test
    void shouldRejectLinkLocalIpAddress()
            throws Exception {

        stubIntegrationConfig();

        config.setBaseUrl(
                "https://169.254.169.254");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(transport, never())
                .get(any(), any(), any());
    }

    // ============================================================
    // Project key validation
    // ============================================================

    @Test
    void shouldRejectInvalidProjectKey()
            throws Exception {

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        "invalid project key"));

        verify(
                integrationConfigRepository,
                never())
                .findByProjectIdAndProvider(
                        any(),
                        eq(IntegrationProvider.JIRA));

        verify(
                transport,
                never())
                .get(any(), any(), any());

        verify(
                secretService,
                never())
                .decrypt(any());
    }

    @Test
    void shouldRejectNullProjectKey()
            throws Exception {

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        null));

        verify(
                integrationConfigRepository,
                never())
                .findByProjectIdAndProvider(
                        any(),
                        eq(IntegrationProvider.JIRA));

        verify(
                transport,
                never())
                .get(any(), any(), any());

        verify(
                secretService,
                never())
                .decrypt(any());
    }

    @Test
    void shouldRejectProjectKeyStartingWithNumber()
            throws Exception {

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        "1PROJ"));

        verify(
                integrationConfigRepository,
                never())
                .findByProjectIdAndProvider(
                        any(),
                        eq(IntegrationProvider.JIRA));

        verify(
                transport,
                never())
                .get(any(), any(), any());

        verify(
                secretService,
                never())
                .decrypt(any());
    }

    // ============================================================
    // Configuration validation
    // ============================================================

    @Test
    void shouldRejectMissingJiraSecret()
            throws Exception {

        stubIntegrationConfig();

        config.setEncryptedSecret(null);

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(
                secretService,
                never())
                .decrypt(any());

        verify(
                transport,
                never())
                .get(any(), any(), any());
    }

    @Test
    void shouldRejectBlankJiraSecret()
            throws Exception {

        stubIntegrationConfig();

        config.setEncryptedSecret("");

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(
                secretService,
                never())
                .decrypt(any());

        verify(
                transport,
                never())
                .get(any(), any(), any());
    }

    @Test
    void shouldRejectMissingAccountIdentifier()
            throws Exception {

        stubIntegrationConfig();

        config.setAccountIdentifier(null);

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(
                transport,
                never())
                .get(any(), any(), any());
    }

    @Test
    void shouldRejectBlankAccountIdentifier()
            throws Exception {

        stubIntegrationConfig();

        config.setAccountIdentifier("");

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));

        verify(
                transport,
                never())
                .get(any(), any(), any());
    }

    // ============================================================
    // Jira server errors
    // ============================================================

    @Test
    void shouldThrowConnectionExceptionWhenJiraReturns500()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                500,
                                "{\"message\":\"Internal Server Error\"}",
                                Map.of()));

        assertThrows(
                JiraConnectionException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }

    @Test
    void shouldThrowClientExceptionForOther4xx()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                400,
                                "{\"message\":\"Bad Request\"}",
                                Map.of()));

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }

    // ============================================================
    // Invalid JSON
    // ============================================================

    @Test
    void shouldThrowClientExceptionWhenJiraReturnsInvalidJson()
            throws Exception {

        stubIntegrationConfig();

        when(secretService.decrypt(
                ENCRYPTED_SECRET))
                .thenReturn(SECRET_TOKEN);

        when(transport.get(
                eq(BASE_URL
                        + "/rest/api/3/project/PROJ"),
                any(),
                eq(Duration.ofSeconds(10))))
                .thenReturn(
                        new JiraHttpResponse(
                                200,
                                "INVALID_JSON",
                                Map.of()));

        assertThrows(
                JiraClientException.class,
                () -> client.getProject(
                        PROJECT_ID,
                        PROJECT_KEY));
    }
}
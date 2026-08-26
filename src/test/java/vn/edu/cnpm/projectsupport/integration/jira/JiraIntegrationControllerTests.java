package vn.edu.cnpm.projectsupport.integration.jira;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.cnpm.projectsupport.common.exception.GlobalExceptionHandler;
import vn.edu.cnpm.projectsupport.common.exception.ResourceNotFoundException;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraAuthType;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionRequest;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraConnectionTestResponse;
import vn.edu.cnpm.projectsupport.integration.jira.contract.JiraIntegrationService;
import vn.edu.cnpm.projectsupport.security.JwtTokenProvider;

@WebMvcTest(JiraIntegrationController.class)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class JiraIntegrationControllerTests {

    private static final Long PROJECT_ID = 10L;
    private static final String BASE_URL = "/api/v1/projects/{projectId}/integrations/jira";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JiraIntegrationService jiraIntegrationService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    private JiraConnectionResponse connectionResponse;
    private JiraConnectionTestResponse testResponse;

    @BeforeEach
    void setUp() {
        connectionResponse = new JiraConnectionResponse(
                PROJECT_ID,
                "https://example.atlassian.net",
                "10000",
                "CNPM",
                JiraAuthType.API_TOKEN,
                true,
                Instant.parse("2026-08-23T16:00:00Z"),
                true
        );

        testResponse = new JiraConnectionTestResponse(
                PROJECT_ID,
                true,
                "account-id-12345",
                "Jira Admin",
                "10000",
                "CNPM",
                Instant.parse("2026-08-23T16:00:00Z"),
                null,
                "Kết nối Jira Cloud thành công"
        );
    }

    @Nested
    @DisplayName("GET /config - Lấy cấu hình Jira")
    class GetConnectionTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminGetsConfigSuccessfully() throws Exception {
            when(jiraIntegrationService.getConnection(PROJECT_ID)).thenReturn(connectionResponse);

            mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectId").value(PROJECT_ID))
                    .andExpect(jsonPath("$.data.siteUrl").value("https://example.atlassian.net"))
                    .andExpect(jsonPath("$.data.projectKey").value("CNPM"))
                    .andExpect(jsonPath("$.data.configured").value(true))
                    .andExpect(jsonPath("$.data.apiToken").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        void teamLeaderCanViewConfig() throws Exception {
            when(jiraIntegrationService.getConnection(PROJECT_ID)).thenReturn(connectionResponse);

            mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.configured").value(true));
        }

        @Test
        @WithMockUser(roles = "TEAM_MEMBER")
        void memberCannotViewConfig() throws Exception {
            mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID))
                    .andExpect(status().isForbidden());

            verify(jiraIntegrationService, never()).getConnection(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void configNotFoundReturns404() throws Exception {
            when(jiraIntegrationService.getConnection(PROJECT_ID))
                    .thenThrow(new ResourceNotFoundException("Jira config not found"));

            mockMvc.perform(get(BASE_URL + "/config", PROJECT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /config - Lưu cấu hình Jira")
    class ConfigureConnectionTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminConfiguresJiraSuccessfully() throws Exception {
            when(jiraIntegrationService.configureConnection(eq(PROJECT_ID), any(JiraConnectionRequest.class)))
                    .thenReturn(connectionResponse);

            mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "siteUrl": "https://example.atlassian.net",
                                      "projectKey": "CNPM",
                                      "email": "admin@example.com",
                                      "apiToken": "secret-token-12345",
                                      "authType": "API_TOKEN"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.projectKey").value("CNPM"))
                    .andExpect(jsonPath("$.data.apiToken").doesNotExist())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(jiraIntegrationService).configureConnection(eq(PROJECT_ID), any(JiraConnectionRequest.class));
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        void teamLeaderCannotConfigureJira() throws Exception {
            mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "siteUrl": "https://example.atlassian.net",
                                      "projectKey": "CNPM",
                                      "email": "admin@example.com",
                                      "apiToken": "secret-token-12345",
                                      "authType": "API_TOKEN"
                                    }
                                    """))
                    .andExpect(status().isForbidden());

            verify(jiraIntegrationService, never()).configureConnection(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void invalidSiteUrlReturnsBadRequest() throws Exception {
            mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "siteUrl": "http://insecure-url.com/path",
                                      "projectKey": "CNPM",
                                      "email": "admin@example.com",
                                      "apiToken": "token",
                                      "authType": "API_TOKEN"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(jiraIntegrationService, never()).configureConnection(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void invalidProjectKeyReturnsBadRequest() throws Exception {
            mockMvc.perform(put(BASE_URL + "/config", PROJECT_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "siteUrl": "https://example.atlassian.net",
                                      "projectKey": "invalid_lower_key",
                                      "email": "admin@example.com",
                                      "apiToken": "token",
                                      "authType": "API_TOKEN"
                                    }
                                    """))
                    .andExpect(status().isBadRequest());

            verify(jiraIntegrationService, never()).configureConnection(any(), any());
        }
    }

    @Nested
    @DisplayName("POST /test-connection - Kiểm tra kết nối Jira")
    class TestConnectionTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        void adminTestsConnectionSuccessfully() throws Exception {
            when(jiraIntegrationService.testConnection(PROJECT_ID)).thenReturn(testResponse);

            mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.connected").value(true))
                    .andExpect(jsonPath("$.data.accountId").value("account-id-12345"))
                    .andExpect(jsonPath("$.data.displayName").value("Jira Admin"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(jiraIntegrationService).testConnection(PROJECT_ID);
        }

        @Test
        @WithMockUser(roles = "TEAM_LEADER")
        void teamLeaderCannotTestConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(jiraIntegrationService, never()).testConnection(any());
        }

        @Test
        @WithMockUser(roles = "TEAM_MEMBER")
        void memberCannotTestConnection() throws Exception {
            mockMvc.perform(post(BASE_URL + "/test-connection", PROJECT_ID)
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            verify(jiraIntegrationService, never()).testConnection(any());
        }
    }
}
package vn.edu.cnpm.projectsupport.integration.jira;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.net.URLEncoder;

import org.springframework.stereotype.Service;

import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueRequest;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraCreateIssueResponse;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
public class JiraRestClient implements JiraClient {

    private static final Pattern PROJECT_KEY_PATTERN =
            Pattern.compile("[A-Z][A-Z0-9_]{1,29}");

    private static final Duration DEFAULT_TIMEOUT =
            Duration.ofSeconds(10);

    private static final Duration MAX_TIMEOUT =
            Duration.ofMinutes(2);

    private final IntegrationConfigRepository integrationConfigRepository;
    private final IntegrationSecretService secretService;
    private final JiraHttpTransport transport;
    private final ObjectMapper objectMapper;

    public JiraRestClient(
            IntegrationConfigRepository integrationConfigRepository,
            IntegrationSecretService secretService,
            JiraHttpTransport transport,
            ObjectMapper objectMapper) {

        this.integrationConfigRepository = integrationConfigRepository;
        this.secretService = secretService;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    @Override
    public JiraConnectionResult testConnection(
            Long projectId,
            String projectKey) {

        IntegrationConfig config =
                getIntegrationConfig(projectId);

        validateProjectKey(projectKey);

        get(
                config,
                "/rest/api/3/myself");

        JiraProject project =
                getProject(projectId, projectKey);

        get(
                config,
                "/rest/api/3/issue/createmeta?projectKeys="
                        + projectKey
                        + "&expand=projects.issuetypes.fields");

        return new JiraConnectionResult(
                true,
                project.id(),
                project.key(),
                project.name());
    }

    @Override
    public JiraProject getProject(
            Long projectId,
            String projectKey) {

        validateProjectKey(projectKey);

        IntegrationConfig config =
                getIntegrationConfig(projectId);

        JsonNode node =
                get(
                        config,
                        "/rest/api/3/project/"
                                + projectKey);

        return new JiraProject(
                text(node, "id"),
                text(node, "key"),
                text(node, "name"),
                text(node, "self"));
    }

    @Override
    public JiraCreateIssueResponse createIssue(
            Long projectId,
            String projectKey,
            JiraCreateIssueRequest request) {

        validateProjectKey(projectKey);

        if (request == null) {
            throw new JiraClientException(
                    "Jira create issue request không được null");
        }

        if (request.summary() == null
                || request.summary().isBlank()) {

            throw new JiraClientException(
                    "Jira issue summary không được để trống");
        }

        if (request.issueType() == null
                || request.issueType().isBlank()) {

            throw new JiraClientException(
                    "Jira issue type không được để trống");
        }

        IntegrationConfig config =
                getIntegrationConfig(projectId);

        /*
         * Tạo JSON body theo Jira REST API.
         */
        Map<String, Object> fields =
                new HashMap<>();

        fields.put(
                "project",
                Map.of("key", projectKey));

        fields.put(
                "summary",
                request.summary());

        fields.put(
                "issuetype",
                Map.of("name", request.issueType()));

        if (request.description() != null
                && !request.description().isBlank()) {

            fields.put(
                    "description",
                    Map.of(
                            "type", "doc",
                            "version", 1,
                            "content", List.of(
                                    Map.of(
                                            "type", "paragraph",
                                            "content", List.of(
                                                    Map.of(
                                                            "type", "text",
                                                            "text", request.description()
                                                    )
                                            )
                                    )
                            )
                    )
            );
        }

        if (request.priority() != null
                && !request.priority().isBlank()) {

            fields.put(
                    "priority",
                    Map.of(
                            "name",
                            request.priority()));
        }

        if (request.labels() != null && !request.labels().isEmpty()) {
            fields.put("labels", request.labels());
        }

        Map<String, Object> payload =
                Map.of("fields", fields);

        String body;

        try {

            body =
                    objectMapper
                            .writeValueAsString(payload);

        } catch (Exception exception) {

            throw new JiraClientException(
                    "Không thể tạo Jira request body",
                    exception);
        }

        JsonNode response =
                post(
                        config,
                        "/rest/api/3/issue",
                        body);

        return new JiraCreateIssueResponse(
                text(response, "id"),
                text(response, "key"),
                text(response, "self"));
    }

    @Override
    public List<JiraCreateIssueResponse> findIssuesByLabel(
            Long projectId,
            String projectKey,
            String label) {

        validateProjectKey(projectKey);
        if (label == null || label.isBlank()) {
            throw new JiraClientException("Jira label không được để trống");
        }

        String encodedJql = URLEncoder.encode(
                "project = " + projectKey + " AND labels = \"" + label.trim() + "\"",
                StandardCharsets.UTF_8);

        IntegrationConfig config = getIntegrationConfig(projectId);
        JsonNode response = get(
                config,
                "/rest/api/3/search/jql?jql=" + encodedJql
                        + "&maxResults=2&fields=summary,status");

        JsonNode issues = response.get("issues");
        if (issues == null || !issues.isArray() || issues.isEmpty()) {
            return List.of();
        }

        List<JiraCreateIssueResponse> result = new java.util.ArrayList<>();
        for (JsonNode issue : issues) {
            result.add(new JiraCreateIssueResponse(
                    text(issue, "id"),
                    text(issue, "key"),
                    text(issue, "self")));
        }
        return result;
    }

    @Override
    public void updateIssue(
            Long projectId,
            String jiraIssueId,
            JiraCreateIssueRequest request) {

        if (jiraIssueId == null || jiraIssueId.isBlank()) {
            throw new JiraClientException("Jira issue id không được để trống");
        }
        if (request == null) {
            throw new JiraClientException("Jira update request không được null");
        }

        IntegrationConfig config = getIntegrationConfig(projectId);
        Map<String, Object> fields = new HashMap<>();
        fields.put("summary", request.summary());

        if (request.description() != null) {
            fields.put("description", Map.of(
                    "type", "doc",
                    "version", 1,
                    "content", List.of(Map.of(
                            "type", "paragraph",
                            "content", List.of(Map.of(
                                    "type", "text",
                                    "text", request.description()))))));
        }

        if (request.priority() != null && !request.priority().isBlank()) {
            fields.put("priority", Map.of("name", request.priority()));
        }
        if (request.labels() != null && !request.labels().isEmpty()) {
            fields.put("labels", request.labels());
        }

        try {
            String body = objectMapper.writeValueAsString(Map.of("fields", fields));
            put(config, "/rest/api/3/issue/" + jiraIssueId, body);
        } catch (JiraApiException e) {
            throw e;
        } catch (Exception e) {
            throw new JiraClientException("Không thể cập nhật Jira issue", e);
        }
    }

    private IntegrationConfig getIntegrationConfig(
            Long projectId) {

        if (projectId == null || projectId <= 0) {

            throw new JiraClientException(
                    "Project ID không hợp lệ");
        }

        return integrationConfigRepository
                .findByProjectIdAndProvider(
                        projectId,
                        IntegrationProvider.JIRA)
                .orElseThrow(() ->
                        new JiraClientException(
                                "Jira integration chưa được cấu hình cho project"));
    }

    private JsonNode get(
            IntegrationConfig config,
            String path) {

        String baseUrl =
                normalizeBaseUrl(config.getBaseUrl());

        validateResolvedHost(baseUrl);

        String encryptedSecret =
                config.getEncryptedSecret();

        if (encryptedSecret == null
                || encryptedSecret.isBlank()) {

            throw new JiraClientException(
                    "Jira secret chưa được cấu hình");
        }

        String token =
                secretService.decrypt(
                        encryptedSecret);

        if (token == null || token.isBlank()) {

            throw new JiraClientException(
                    "Jira secret chưa được cấu hình");
        }

        String accountIdentifier =
                config.getAccountIdentifier();

        if (accountIdentifier == null
                || accountIdentifier.isBlank()) {

            throw new JiraClientException(
                    "Jira account identifier chưa được cấu hình");
        }

        String credentials =
                accountIdentifier + ":" + token;

        String basicAuth =
                Base64.getEncoder()
                        .encodeToString(
                                credentials.getBytes(
                                        StandardCharsets.UTF_8));

        Map<String, String> headers =
                new HashMap<>();

        headers.put(
                "Authorization",
                "Basic " + basicAuth);

        headers.put(
                "Accept",
                "application/json");

        try {

            JiraHttpResponse response =
                    transport.get(
                            baseUrl + path,
                            headers,
                            safeTimeout());

            return handleResponse(response);

        } catch (JiraApiException exception) {

            throw exception;

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new JiraConnectionException(
                    "Không thể kết nối Jira",
                    exception);

        } catch (IOException exception) {

            throw new JiraConnectionException(
                    "Không thể kết nối Jira",
                    exception);

        } catch (RuntimeException exception) {

            throw new JiraClientException(
                    "Không thể gọi Jira",
                    exception);
        }
    }

    private JsonNode post(
            IntegrationConfig config,
            String path,
            String body) {

        String baseUrl =
                normalizeBaseUrl(config.getBaseUrl());

        validateResolvedHost(baseUrl);

        String encryptedSecret =
                config.getEncryptedSecret();

        if (encryptedSecret == null
                || encryptedSecret.isBlank()) {

            throw new JiraClientException(
                    "Jira secret chưa được cấu hình");
        }

        String token =
                secretService.decrypt(
                        encryptedSecret);

        if (token == null || token.isBlank()) {

            throw new JiraClientException(
                    "Jira secret chưa được cấu hình");
        }

        String accountIdentifier =
                config.getAccountIdentifier();

        if (accountIdentifier == null
                || accountIdentifier.isBlank()) {

            throw new JiraClientException(
                    "Jira account identifier chưa được cấu hình");
        }

        String credentials =
                accountIdentifier + ":" + token;

        String basicAuth =
                Base64.getEncoder()
                        .encodeToString(
                                credentials.getBytes(
                                        StandardCharsets.UTF_8));

        Map<String, String> headers =
                new HashMap<>();

        headers.put(
                "Authorization",
                "Basic " + basicAuth);

        headers.put(
                "Accept",
                "application/json");

        headers.put(
                "Content-Type",
                "application/json");

        try {

            JiraHttpResponse response =
                    transport.post(
                            baseUrl + path,
                            headers,
                            body,
                            safeTimeout());

            return handleResponse(response);

        } catch (JiraApiException exception) {

            throw exception;

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new JiraConnectionException(
                    "Không thể kết nối Jira",
                    exception);

        } catch (IOException exception) {

            throw new JiraConnectionException(
                    "Không thể kết nối Jira",
                    exception);

        } catch (RuntimeException exception) {

            throw new JiraClientException(
                    "Không thể gọi Jira",
                    exception);
        }
    }

    private JsonNode put(
            IntegrationConfig config,
            String path,
            String body) {

        String baseUrl = normalizeBaseUrl(config.getBaseUrl());
        validateResolvedHost(baseUrl);

        String encryptedSecret = config.getEncryptedSecret();
        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            throw new JiraClientException("Jira secret chưa được cấu hình");
        }

        String token = secretService.decrypt(encryptedSecret);
        String accountIdentifier = config.getAccountIdentifier();
        if (token == null || token.isBlank()
                || accountIdentifier == null || accountIdentifier.isBlank()) {
            throw new JiraClientException("Jira credentials chưa được cấu hình");
        }

        String basicAuth = Base64.getEncoder().encodeToString(
                (accountIdentifier + ":" + token).getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + basicAuth);
        headers.put("Accept", "application/json");
        headers.put("Content-Type", "application/json");

        try {
            JiraHttpResponse response =
                    transport.put(baseUrl + path, headers, body, safeTimeout());
            return handleResponse(response);
        } catch (JiraApiException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JiraConnectionException("Không thể kết nối Jira", e);
        } catch (IOException e) {
            throw new JiraConnectionException("Không thể kết nối Jira", e);
        }
    }

    private JsonNode handleResponse(
            JiraHttpResponse response) {

        int status =
                response.statusCode();

        if (status == 401) {

            throw new JiraAuthenticationException(
                    "Jira authentication thất bại");
        }

        if (status == 403) {

            throw new JiraAuthorizationException(
                    "Tài khoản không có quyền truy cập Jira resource");
        }

        if (status == 404) {

            throw new JiraProjectNotFoundException(
                    "Jira project/resource không tồn tại");
        }

        if (status == 429) {

            return throwRateLimit(response);
        }

        if (status >= 500) {

            throw new JiraConnectionException(
                    "Jira đang không khả dụng");
        }

        if (status < 200 || status >= 300) {

            throw new JiraClientException(
                    "Jira request thất bại");
        }

        try {

            return objectMapper.readTree(
                    response.body() == null
                            ? "{}"
                            : response.body());

        } catch (Exception exception) {

            throw new JiraClientException(
                    "Jira trả về dữ liệu không hợp lệ",
                    exception);
        }
    }

    private JsonNode throwRateLimit(
            JiraHttpResponse response) {

        String retryAfter =
                response.headers().get("retry-after");

        Duration retry =
                Duration.ZERO;

        if (retryAfter != null) {

            try {

                long seconds =
                        Long.parseLong(
                                retryAfter.trim());

                if (seconds >= 0) {

                    retry =
                            Duration.ofSeconds(seconds);
                }

            } catch (NumberFormatException ignored) {
            }
        }

        throw new JiraRateLimitException(
                "Jira rate limit exceeded",
                retry);
    }

    private String normalizeBaseUrl(
            String raw) {

        if (raw == null || raw.isBlank()) {

            throw new JiraClientException(
                    "Jira base URL chưa được cấu hình");
        }

        String normalized =
                raw.trim();

        try {

            URI uri =
                    URI.create(normalized);

            String scheme =
                    uri.getScheme();

            if (!"https".equalsIgnoreCase(scheme)) {

                throw new JiraClientException(
                        "Jira base URL phải sử dụng HTTPS");
            }

            int port =
                    uri.getPort();

            if (port != -1 && port != 443) {

                throw new JiraClientException(
                        "Jira base URL phải sử dụng port HTTPS mặc định 443");
            }

            if (uri.getHost() == null
                    || uri.getHost().isBlank()) {

                throw new JiraClientException(
                        "Jira base URL không hợp lệ");
            }

            if (uri.getUserInfo() != null) {

                throw new JiraClientException(
                        "Jira base URL không được chứa user information");
            }

            if (uri.getQuery() != null
                    || uri.getFragment() != null) {

                throw new JiraClientException(
                        "Jira base URL không được chứa query hoặc fragment");
            }

            String path =
                    uri.getPath();

            if (path != null
                    && !path.isBlank()
                    && !"/".equals(path)) {

                throw new JiraClientException(
                        "Jira base URL phải là HTTPS origin");
            }

            return normalized.replaceAll("/+$", "");

        } catch (JiraClientException exception) {

            throw exception;

        } catch (IllegalArgumentException exception) {

            throw new JiraClientException(
                    "Jira base URL không hợp lệ",
                    exception);
        }
    }

    private void validateResolvedHost(
            String baseUrl) {

        try {

            URI uri =
                    URI.create(baseUrl);

            String host =
                    uri.getHost();

            if (host == null || host.isBlank()) {

                throw new JiraClientException(
                        "Jira host không hợp lệ");
            }

            InetAddress[] addresses =
                    InetAddress.getAllByName(host);

            if (addresses.length == 0) {

                throw new JiraClientException(
                        "Không thể resolve Jira host");
            }

            for (InetAddress address : addresses) {

                if (isUnsafeAddress(address)) {

                    throw new JiraClientException(
                            "Jira host trỏ tới địa chỉ mạng không được phép");
                }
            }

        } catch (UnknownHostException exception) {

            throw new JiraClientException(
                    "Không thể resolve Jira host",
                    exception);

        } catch (IllegalArgumentException exception) {

            throw new JiraClientException(
                    "Jira host không hợp lệ",
                    exception);
        }
    }

    private boolean isUnsafeAddress(
            InetAddress address) {

        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }

    private Duration safeTimeout() {

        return DEFAULT_TIMEOUT.compareTo(MAX_TIMEOUT) > 0
                ? MAX_TIMEOUT
                : DEFAULT_TIMEOUT;
    }

    private void validateProjectKey(
            String projectKey) {

        if (projectKey == null
                || !PROJECT_KEY_PATTERN
                        .matcher(projectKey.trim())
                        .matches()) {

            throw new JiraClientException(
                    "Jira Project Key không hợp lệ");
        }
    }

    private String text(
            JsonNode node,
            String field) {

        JsonNode value =
                node.get(field);

        return value == null || value.isNull()
                ? null
                : value.asText();
    }
}
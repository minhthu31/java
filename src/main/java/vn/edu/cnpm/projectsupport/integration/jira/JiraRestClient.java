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
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraAdfDocumentDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueTypeDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraParentIssueDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraIssueFieldsDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraPriorityDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraProjectDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraSprintPageDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraStatusDto;
import vn.edu.cnpm.projectsupport.integration.jira.dto.JiraUserDto;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
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

        /*
         * Validate project key trước khi đưa vào URL.
         */
        validateProjectKey(projectKey);

        /*
         * 1. Kiểm tra authentication với Jira.
         */
        get(
                config,
                "/rest/api/3/myself");

        /*
         * 2. Kiểm tra project có tồn tại và account
         *    có quyền truy cập project hay không.
         */
        JiraProject project =
                getProject(projectId, projectKey);

        /*
         * 3. Kiểm tra Create Metadata.
         *
         * Endpoint này xác nhận integration có thể
         * truy cập metadata cần thiết cho việc tạo Jira issue.
         */
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
    public JiraPageDto<JiraIssueDto> getIssues(
            Long projectId, String projectKey, int startAt, int maxResults) {
        validatePage(startAt, maxResults);
        validateProjectKey(projectKey);
        IntegrationConfig config = getIntegrationConfig(projectId);
        JsonNode node = get(config, "/rest/api/3/search?jql=project%3D"
                + projectKey + "&startAt=" + startAt + "&maxResults=" + maxResults);
        return toIssuePage(node);
    }

    @Override
    public JiraPageDto<JiraIssueDto> getBacklog(
            Long projectId, String projectKey, int startAt, int maxResults) {
        validatePage(startAt, maxResults);
        validateProjectKey(projectKey);
        IntegrationConfig config = getIntegrationConfig(projectId);
        long boardId = findBoardId(config, projectKey);
        JsonNode node = get(config, "/rest/agile/1.0/board/" + boardId
                + "/backlog?startAt=" + startAt + "&maxResults=" + maxResults);
        return toIssuePage(node);
    }

    @Override
    public JiraSprintPageDto getSprints(
            Long projectId, String projectKey, int startAt, int maxResults) {
        validatePage(startAt, maxResults);
        validateProjectKey(projectKey);
        IntegrationConfig config = getIntegrationConfig(projectId);
        long boardId = findBoardId(config, projectKey);
        JsonNode node = get(config, "/rest/agile/1.0/board/" + boardId
                + "/sprint?startAt=" + startAt + "&maxResults=" + maxResults);
        return toSprintPage(node);
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

    private void validatePage(int startAt, int maxResults) {
        if (startAt < 0 || maxResults <= 0 || maxResults > 100) {
            throw new JiraClientException("Pagination không hợp lệ");
        }
    }

    private long findBoardId(IntegrationConfig config, String projectKey) {
        JsonNode node = get(config, "/rest/agile/1.0/board?projectKeyOrId="
                + projectKey + "&startAt=0&maxResults=50");
        JsonNode values = node.get("values");
        if (values == null || !values.isArray() || values.isEmpty()) {
            throw new JiraClientException("Jira project chưa có Scrum/Kanban board");
        }
        JsonNode id = values.get(0).get("id");
        if (id == null || id.isNull()) {
            throw new JiraClientException("Jira board không có id hợp lệ");
        }
        return id.asLong();
    }

    private JiraPageDto<JiraIssueDto> toIssuePage(JsonNode node) {
        int startAt = number(node, "startAt", 0);
        int maxResults = number(node, "maxResults", 0);
        int total = number(node, "total", startAt);
        Boolean isLast = node.has("isLast") && !node.get("isLast").isNull()
                ? node.get("isLast").asBoolean() : null;
        List<JiraIssueDto> issues = new ArrayList<>();
        JsonNode array = node.get("issues");
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                issues.add(toIssueDto(item));
            }
        }
        return new JiraPageDto<>(startAt, maxResults, total, isLast, issues);
    }

    private JiraIssueDto toIssueDto(JsonNode node) {
        JsonNode fields = node.get("fields");
        String id = text(node, "id");
        String key = text(node, "key");
        String summary = text(fields, "summary");
        JiraStatusDto status = null;
        JsonNode statusNode = fields == null ? null : fields.get("status");
        if (statusNode != null && !statusNode.isNull()) {
            status = new JiraStatusDto(text(statusNode, "id"), text(statusNode, "name"));
        }
        JiraPriorityDto priority = null;
        JsonNode priorityNode = fields == null ? null : fields.get("priority");
        if (priorityNode != null && !priorityNode.isNull()) {
            priority = new JiraPriorityDto(text(priorityNode, "id"), text(priorityNode, "name"));
        }
        JiraUserDto assignee = null;
        JsonNode assigneeNode = fields == null ? null : fields.get("assignee");
        if (assigneeNode != null && !assigneeNode.isNull()) {
            assignee = new JiraUserDto(text(assigneeNode, "accountId"), text(assigneeNode, "displayName"),
                    text(assigneeNode, "emailAddress"), assigneeNode.path("active").asBoolean(false));
        }
        JiraProjectDto project = null;
        JsonNode projectNode = fields == null ? null : fields.get("project");
        if (projectNode != null && !projectNode.isNull()) {
            project = new JiraProjectDto(
                    text(projectNode, "id"),
                    text(projectNode, "key"),
                    text(projectNode, "name"));
        }

        JiraIssueTypeDto issueType = null;
        JsonNode issueTypeNode = fields == null ? null : fields.get("issuetype");
        if (issueTypeNode != null && !issueTypeNode.isNull()) {
            issueType = new JiraIssueTypeDto(
                    text(issueTypeNode, "id"),
                    text(issueTypeNode, "name"));
        }

        java.time.LocalDate dueDate = null;
        String dueDateText = text(fields, "duedate");
        if (dueDateText != null && !dueDateText.isBlank()) {
            try {
                dueDate = java.time.LocalDate.parse(dueDateText);
            } catch (java.time.format.DateTimeParseException ignored) {
                // Jira returned an unexpected date format; keep the optional field null.
            }
        }

        JiraParentIssueDto parent = null;
        JsonNode parentNode = fields == null ? null : fields.get("parent");
        if (parentNode != null && !parentNode.isNull()) {
            parent = new JiraParentIssueDto(
                    text(parentNode, "id"),
                    text(parentNode, "key"));
        }

        String updated = text(fields, "updated");
        JiraAdfDocumentDto description = null;
        JsonNode descriptionNode = fields == null ? null : fields.get("description");
        if (descriptionNode != null && !descriptionNode.isNull()) {
            try {
                description = objectMapper.readValue(
                        descriptionNode.toString(), JiraAdfDocumentDto.class);
            } catch (Exception ignored) {
                // Keep description null when Jira returns an unsupported ADF shape.
            }
        }

        return new JiraIssueDto(
                id,
                key,
                new JiraIssueFieldsDto(
                        summary,
                        description,
                        status,
                        priority,
                        assignee,
                        project,
                        issueType,
                        dueDate,
                        parent,
                        updated));
    }

    private JiraSprintPageDto toSprintPage(JsonNode node) {
        int startAt = number(node, "startAt", 0);
        int maxResults = number(node, "maxResults", 0);
        int total = number(node, "total", startAt);
        Boolean isLast = node.has("isLast") ? node.get("isLast").asBoolean() : null;
        List<JiraSprintDto> values = new ArrayList<>();
        JsonNode array = node.get("values");
        if (array != null && array.isArray()) {
            for (JsonNode item : array) {
                values.add(new JiraSprintDto(text(item, "id"), text(item, "name"), text(item, "state"),
                        text(item, "goal"), text(item, "startDate"), text(item, "endDate")));
            }
        }
        return new JiraSprintPageDto(startAt, maxResults, total, isLast, values);
    }

    private int number(JsonNode node, String field, int fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asInt();
    }

    private IntegrationConfig getIntegrationConfig(
            Long projectId) {

        if (projectId == null || projectId <= 0) {
            throw new JiraClientException("Project ID không hợp lệ");
        }

        return integrationConfigRepository.findByProjectIdAndProvider(projectId, IntegrationProvider.JIRA).orElseThrow(() ->
                        new JiraClientException("Jira integration chưa được cấu hình cho project"));
    }

    private JsonNode get(IntegrationConfig config, String path) {

        String baseUrl = normalizeBaseUrl(config.getBaseUrl());

        validateResolvedHost(baseUrl);

        String encryptedSecret =
                config.getEncryptedSecret();

        if (encryptedSecret == null || encryptedSecret.isBlank()) {
            throw new JiraClientException("Jira secret chưa được cấu hình");
        }

        String token = secretService.decrypt(encryptedSecret);

        if (token == null || token.isBlank()) {
            throw new JiraClientException("Jira secret chưa được cấu hình");
        }


        String accountIdentifier = config.getAccountIdentifier();

        if (accountIdentifier == null || accountIdentifier.isBlank()) {
            throw new JiraClientException("Jira account identifier chưa được cấu hình");
        }

        String credentials = accountIdentifier + ":" + token;

        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();

        headers.put("Authorization","Basic " + basicAuth);
        headers.put("Accept", "application/json");

        try {
            JiraHttpResponse response = transport.get(baseUrl + path, headers, safeTimeout());
            return handleResponse(response);

        } catch (JiraApiException exception) {

            throw exception;

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();
            throw new JiraConnectionException("Không thể kết nối Jira", exception);

        } catch (IOException exception) {
            throw new JiraConnectionException("Không thể kết nối Jira", exception);

        } catch (RuntimeException exception) {

            throw new JiraClientException("Không thể gọi Jira", exception);
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
                /*
                 * Giữ retry = ZERO nếu header không hợp lệ.
                 */
            }
        }

        throw new JiraRateLimitException(
                "Jira rate limit exceeded",
                retry);
    }

    /**
     * Chỉ chấp nhận HTTPS origin hợp lệ.
     *
     * Hợp lệ:
     * https://example.atlassian.net
     * https://example.atlassian.net/
     * https://example.atlassian.net:443
     *
     * Không hợp lệ:
     * http://example.atlassian.net
     * https://example.atlassian.net:8080
     * https://example.atlassian.net:8443
     * https://example.atlassian.net/path
     * https://example.atlassian.net?x=1
     * https://example.atlassian.net#fragment
     * https://user:password@example.atlassian.net
     */
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

            /*
             * Chỉ HTTPS.
             */
            String scheme =
                    uri.getScheme();

            if (!"https".equalsIgnoreCase(scheme)) {

                throw new JiraClientException(
                        "Jira base URL phải sử dụng HTTPS");
            }

            /*
             * Chỉ cho phép port HTTPS mặc định 443.
             *
             * - Không ghi port: -1 -> hợp lệ.
             * - :443 -> hợp lệ.
             * - :8080 -> không hợp lệ.
             * - :8443 -> không hợp lệ.
             */
            int port =
                    uri.getPort();

            if (port != -1 && port != 443) {

                throw new JiraClientException(
                        "Jira base URL phải sử dụng port HTTPS mặc định 443");
            }

            /*
             * Phải có host.
             */
            if (uri.getHost() == null
                    || uri.getHost().isBlank()) {

                throw new JiraClientException(
                        "Jira base URL không hợp lệ");
            }

            /*
             * Không cho phép user-info.
             */
            if (uri.getUserInfo() != null) {

                throw new JiraClientException(
                        "Jira base URL không được chứa user information");
            }

            /*
             * Không cho phép query hoặc fragment.
             */
            if (uri.getQuery() != null
                    || uri.getFragment() != null) {

                throw new JiraClientException(
                        "Jira base URL không được chứa query hoặc fragment");
            }

            /*
             * Chỉ chấp nhận origin.
             *
             * Path chỉ được phép là "/" hoặc rỗng.
             */
            String path =
                    uri.getPath();

            if (path != null
                    && !path.isBlank()
                    && !"/".equals(path)) {

                throw new JiraClientException(
                        "Jira base URL phải là HTTPS origin");
            }

            /*
             * Loại bỏ "/" cuối URL để khi nối endpoint
             * không tạo thành "//rest/api/...".
             */
            return normalized.replaceAll("/+$", "");

        } catch (JiraClientException exception) {

            throw exception;

        } catch (IllegalArgumentException exception) {

            throw new JiraClientException(
                    "Jira base URL không hợp lệ",
                    exception);
        }
    }

    /**
     * Resolve DNS và kiểm tra toàn bộ địa chỉ IP
     * mà hostname trỏ tới.
     *
     * Mục đích:
     * - Chặn loopback.
     * - Chặn private IP.
     * - Chặn link-local.
     * - Chặn multicast.
     * - Chặn wildcard/any-local.
     */
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

            /*
             * Kiểm tra tất cả IP mà hostname resolve tới.
             */
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

    /**
     * Kiểm tra địa chỉ IP có thuộc nhóm không an toàn
     * cho outbound Jira request hay không.
     */
    private boolean isUnsafeAddress(
            InetAddress address) {

        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress();
    }

    /**
     * Đảm bảo timeout không vượt quá giới hạn cho phép.
     */
    private Duration safeTimeout() {

        return DEFAULT_TIMEOUT.compareTo(MAX_TIMEOUT) > 0
                ? MAX_TIMEOUT
                : DEFAULT_TIMEOUT;
    }

    /**
     * Jira Project Key phải:
     * - Bắt đầu bằng chữ in hoa.
     * - Sau đó chỉ được chứa A-Z, 0-9 hoặc _.
     * - Độ dài tối đa 30 ký tự.
     */
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

    /**
     * Lấy text field an toàn từ JSON.
     */
    private String text(
            JsonNode node,
            String field) {

        if (node == null) {
            return null;
        }

        JsonNode value =
                node.get(field);

        return value == null || value.isNull()
                ? null
                : value.asText();
    }
}
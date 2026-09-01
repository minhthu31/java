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
import java.net.URLEncoder;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

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

        String issueTypeId = resolveIssueTypeId(
                config, projectKey, request.issueType());
        fields.put(
                "issuetype",
                Map.of("id", issueTypeId));

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

            String priorityId = resolvePriorityId(
                    config, projectKey, request.issueType(), request.priority());

            fields.put(
                    "priority",
                    Map.of("id", priorityId));
        }

        addMappingFields(config, projectKey, request, fields);

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
            String projectKey,
            String jiraIssueId,
            JiraCreateIssueRequest request) {

        if (jiraIssueId == null || jiraIssueId.isBlank()) {
            throw new JiraClientException("Jira issue id không được để trống");
        }
        if (request == null) {
            throw new JiraClientException("Jira update request không được null");
        }

        validateProjectKey(projectKey);
        IntegrationConfig config = getIntegrationConfig(projectId);
        Map<String, Object> fields = new HashMap<>();
        fields.put("summary", request.summary());

        if (request.issueType() != null && !request.issueType().isBlank()) {
            fields.put("issuetype", Map.of("id",
                    resolveIssueTypeId(config, projectKey, request.issueType())));
        }

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
            fields.put("priority", Map.of("id",
                    resolvePriorityId(config, projectKey, request.issueType(), request.priority())));
        }
        addMappingFields(config, projectKey, request, fields);
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


    private String resolveIssueTypeId(
            IntegrationConfig config,
            String projectKey,
            String issueTypeName) {

        if (issueTypeName == null || issueTypeName.isBlank()) {
            throw new JiraClientException("Jira issue type không được để trống");
        }

        JsonNode meta = get(
                config,
                "/rest/api/3/issue/createmeta?projectKeys="
                        + URLEncoder.encode(projectKey, StandardCharsets.UTF_8)
                        + "&expand=projects.issuetypes.fields");

        JsonNode projects = meta.get("projects");
        if (projects != null && projects.isArray()) {
            for (JsonNode project : projects) {
                JsonNode issueTypes = project.get("issuetypes");
                if (issueTypes != null && issueTypes.isArray()) {
                    for (JsonNode issueType : issueTypes) {
                        if (issueTypeName.equalsIgnoreCase(text(issueType, "name"))) {
                            String id = text(issueType, "id");
                            if (id != null && !id.isBlank()) {
                                return id;
                            }
                        }
                    }
                }
            }
        }

        throw new JiraApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "ISSUE_TYPE_MAPPING_MISSING",
                false,
                null,
                "Không tìm thấy Jira Issue Type trong metadata: " + issueTypeName,
                null);
    }

    private String resolvePriorityId(
            IntegrationConfig config,
            String projectKey,
            String issueTypeName,
            String priorityName) {

        if (priorityName == null || priorityName.isBlank()) {
            throw new JiraClientException("Jira priority không được để trống");
        }

        String key = projectKey;
        if (key == null || key.isBlank()) {
            throw new JiraClientException(
                    "Không thể resolve Jira priority vì thiếu project key");
        }

        JsonNode meta = get(
                config,
                "/rest/api/3/issue/createmeta?projectKeys="
                        + URLEncoder.encode(key, StandardCharsets.UTF_8)
                        + "&expand=projects.issuetypes.fields");

        JsonNode projects = meta.get("projects");
        if (projects != null && projects.isArray()) {
            for (JsonNode project : projects) {
                JsonNode issueTypes = project.get("issuetypes");
                if (issueTypes == null || !issueTypes.isArray()) {
                    continue;
                }

                for (JsonNode issueType : issueTypes) {
                    if (issueTypeName != null
                            && !issueTypeName.equalsIgnoreCase(text(issueType, "name"))) {
                        continue;
                    }

                    JsonNode fields = issueType.get("fields");
                    JsonNode priority = fields == null ? null : fields.get("priority");
                    JsonNode values = priority == null ? null : priority.get("allowedValues");
                    if (values != null && values.isArray()) {
                        for (JsonNode value : values) {
                            if (priorityName.equalsIgnoreCase(text(value, "name"))) {
                                String id = text(value, "id");
                                if (id != null && !id.isBlank()) {
                                    return id;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Some Jira metadata configurations do not expose priority allowedValues.
        JsonNode priorities = get(config, "/rest/api/3/priority");
        if (priorities != null && priorities.isArray()) {
            for (JsonNode priority : priorities) {
                if (priorityName.equalsIgnoreCase(text(priority, "name"))) {
                    String id = text(priority, "id");
                    if (id != null && !id.isBlank()) {
                        return id;
                    }
                }
            }
        }

        throw new JiraApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "PRIORITY_MAPPING_MISSING",
                false,
                null,
                "Không tìm thấy Jira Priority trong metadata: " + priorityName,
                null);
    }

    private void addMappingFields(
            IntegrationConfig config,
            String projectKey,
            JiraCreateIssueRequest request,
            Map<String, Object> fields) {

        if (request.assigneeAccountId() != null
                && !request.assigneeAccountId().isBlank()) {
            fields.put(
                    "assignee",
                    Map.of("accountId", request.assigneeAccountId().trim()));
        }

        if (request.dueDate() != null
                && !request.dueDate().isBlank()) {
            fields.put(
                    "duedate",
                    normalizeDueDate(request.dueDate()));
        }

        if (request.epicKey() != null
                && !request.epicKey().isBlank()
                && !isEpicIssueType(request.issueType())) {
            fields.put(
                    "parent",
                    Map.of("key", request.epicKey().trim()));
        }
    }

    private boolean isEpicIssueType(String issueTypeName) {
        return issueTypeName != null
                && "EPIC".equalsIgnoreCase(issueTypeName.trim());
    }

    @Override
    public void addIssueToSprint(
            Long projectId,
            String jiraSprintId,
            String jiraIssueId) {

        IntegrationConfig config = getIntegrationConfig(projectId);

        if (jiraSprintId == null || jiraSprintId.isBlank()) {
            return;
        }

        if (jiraIssueId == null || jiraIssueId.isBlank()) {
            throw new JiraClientException(
                    "Jira issue id không được để trống khi đưa vào Sprint");
        }

        String path =
                "/rest/agile/1.0/sprint/"
                        + jiraSprintId.trim()
                        + "/issue";

        try {
            String body =
                    objectMapper.writeValueAsString(
                            Map.of("issues", List.of(jiraIssueId)));
            post(config, path, body);
        } catch (JiraApiException e) {
            throw e;
        } catch (Exception e) {
            throw new JiraClientException(
                    "Không thể đưa Jira Issue vào Sprint",
                    e);
        }
    }

    private String normalizeDueDate(String dueDate) {
        try {
            return java.time.Instant.parse(dueDate.trim())
                    .atZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                    .toLocalDate()
                    .toString();
        } catch (Exception ignored) {
            try {
                return java.time.OffsetDateTime.parse(dueDate.trim())
                        .atZoneSameInstant(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
                        .toLocalDate()
                        .toString();
            } catch (Exception e) {
                try {
                    return java.time.LocalDate.parse(dueDate.trim()).toString();
                } catch (Exception ignoredAgain) {
                    throw new JiraClientException("Deadline không hợp lệ: " + dueDate);
                }
            }
        }
    }

    private String resolveCustomFieldId(
            IntegrationConfig config,
            String fieldName) {

        JsonNode fields = get(config, "/rest/api/3/field");
        if (fields != null && fields.isArray()) {
            for (JsonNode field : fields) {
                if (fieldName.equalsIgnoreCase(text(field, "name"))) {
                    String id = text(field, "id");
                    if (id != null && id.startsWith("customfield_")) {
                        return id;
                    }
                }
            }
        }
        return null;
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

            String body = response.body();
            if (body == null || body.isBlank()) {
                return objectMapper.createObjectNode();
            }

            return objectMapper.readTree(body);

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

        if (node == null) {
            return null;
        }

        JsonNode value =
                node.get(field);

        return value == null || value.isNull()
                ? null
                : value.asText();
    }
    @Override
    public void updateIssueAssignee(Long projectId, String projectKey, String jiraIssueKey, String assigneeAccountId) {
        IntegrationConfig config = getIntegrationConfig(projectId);
        String path = "/rest/api/3/issue/" + jiraIssueKey + "/assignee";
        String body = assigneeAccountId == null
                ? "{\"accountId\": null}"
                : "{\"accountId\": \"" + assigneeAccountId + "\"}";
        put(config, path, body);
    }

    @Override
    public void transitionIssueStatus(Long projectId, String projectKey, String jiraIssueKey, String targetStatusName) {
        IntegrationConfig config = getIntegrationConfig(projectId);
        String transitionId = resolveTransitionId(config, jiraIssueKey, targetStatusName);
        String path = "/rest/api/3/issue/" + jiraIssueKey + "/transitions";
        String body = "{\"transition\": {\"id\": \"" + transitionId + "\"}}";
        post(config, path, body);
    }

    private String resolveTransitionId(IntegrationConfig config, String jiraIssueKey, String targetStatusName) {
        String path = "/rest/api/3/issue/" + jiraIssueKey + "/transitions";
        JsonNode node = get(config, path);
        if (node.has("transitions") && node.get("transitions").isArray()) {
            for (JsonNode t : node.get("transitions")) {
                String name = t.path("name").asText();
                String toName = t.path("to").path("name").asText();
                if (targetStatusName.equalsIgnoreCase(name) || targetStatusName.equalsIgnoreCase(toName)) {
                    return t.path("id").asText();
                }
            }
        }
        throw new JiraClientException("JIRA_TRANSITION_MAPPING_MISSING");
    }
}

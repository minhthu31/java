package vn.edu.cnpm.projectsupport.integration.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;

@Service
public class JiraRestClient implements JiraClient {
    private static final Pattern PROJECT_KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{1,49}");

    private final JiraProperties properties;
    private final IntegrationSecretService secretService;
    private final JiraHttpTransport transport;
    private final ObjectMapper objectMapper;

    public JiraRestClient(
            JiraProperties properties,
            IntegrationSecretService secretService,
            JiraHttpTransport transport,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.secretService = secretService;
        this.transport = transport;
        this.objectMapper = objectMapper;
    }

    @Override
    public JiraConnectionResult testConnection() {
        get("/rest/api/3/myself");
        JiraProject project = getProject(properties.getProjectKey());
        return new JiraConnectionResult(true, project.key(), project.name());
    }

    @Override
    public JiraProject getProject(String projectKey) {
        validateProjectKey(projectKey);
        JsonNode node = get("/rest/api/3/project/" + projectKey);
        return new JiraProject(
                text(node, "id"),
                text(node, "key"),
                text(node, "name"),
                text(node, "self"));
    }

    private JsonNode get(String path) {
        String baseUrl = normalizeBaseUrl(properties.getBaseUrl());
        String token = secretService.decrypt(properties.getEncryptedToken());
        String credentials = properties.authenticationIdentifier() + ":" + token;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Basic " + basicAuth);
        headers.put("Accept", "application/json");

        // Do not log token, credentials, headers, or response body.
        try {
            JiraHttpResponse response = transport.get(
                    baseUrl + path, headers, safeTimeout(properties.getTimeout()));
            return handleResponse(response);
        } catch (JiraClientException | JiraAuthenticationException | JiraAuthorizationException
                 | JiraProjectNotFoundException | JiraRateLimitException exception) {
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

    private JsonNode handleResponse(JiraHttpResponse response) {
        int status = response.statusCode();
        if (status == 401) throw new JiraAuthenticationException("Jira authentication thất bại");
        if (status == 403) throw new JiraAuthorizationException("Tài khoản không có quyền truy cập Jira resource");
        if (status == 404) throw new JiraProjectNotFoundException("Jira project/resource không tồn tại");
        if (status == 429) {
            return throwRateLimit(response);
        }
        if (status >= 500) throw new JiraConnectionException("Jira đang không khả dụng");
        if (status < 200 || status >= 300) throw new JiraClientException("Jira request thất bại");
        try {
            return objectMapper.readTree(response.body() == null ? "{}" : response.body());
        } catch (IOException exception) {
            throw new JiraClientException("Jira trả về dữ liệu không hợp lệ", exception);
        }
    }

    private JsonNode throwRateLimit(JiraHttpResponse response) {
        String retryAfter = response.headers().get("retry-after");
        Duration retry = Duration.ZERO;
        if (retryAfter != null) {
            try { retry = Duration.ofSeconds(Long.parseLong(retryAfter)); }
            catch (NumberFormatException ignored) { }
        }
        throw new JiraRateLimitException("Jira rate limit exceeded", retry);
    }

    private String normalizeBaseUrl(String raw) {
        if (raw == null || raw.isBlank()) throw new JiraClientException("Jira base URL chưa được cấu hình");
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme)) {
                throw new JiraClientException("Jira base URL phải sử dụng HTTP hoặc HTTPS");
            }
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new JiraClientException("Jira base URL không hợp lệ");
            }
            return raw.trim().replaceAll("/+$", "");
        } catch (IllegalArgumentException exception) {
            throw new JiraClientException("Jira base URL không hợp lệ", exception);
        }
    }

    private Duration safeTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) return Duration.ofSeconds(10);
        return timeout.compareTo(Duration.ofMinutes(2)) > 0 ? Duration.ofMinutes(2) : timeout;
    }

    private void validateProjectKey(String projectKey) {
        if (projectKey == null || !PROJECT_KEY_PATTERN.matcher(projectKey.trim()).matches()) {
            throw new JiraClientException("Jira Project Key không hợp lệ");
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}

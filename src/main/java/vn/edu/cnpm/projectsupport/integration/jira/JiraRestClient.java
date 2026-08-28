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
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationConfig;
import vn.edu.cnpm.projectsupport.integration.jira.domain.IntegrationProvider;
import vn.edu.cnpm.projectsupport.integration.jira.repository.IntegrationConfigRepository;
import vn.edu.cnpm.projectsupport.security.IntegrationSecretService;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

@Service
public class JiraRestClient implements JiraClient {

    private static final Pattern PROJECT_KEY_PATTERN =
            Pattern.compile("[A-Za-z][A-Za-z0-9_-]{1,49}");

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

        /*
         * Resolve DNS và chặn địa chỉ private/local
         * trước khi thực hiện HTTP request.
         */
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
                // Giữ retry = ZERO nếu header không hợp lệ.
            }
        }

        throw new JiraRateLimitException(
                "Jira rate limit exceeded",
                retry);
    }

    /**
     * Chỉ chấp nhận HTTPS origin hợp lệ.
     *
     * Ví dụ hợp lệ:
     * https://example.atlassian.net
     * https://example.atlassian.net/
     *
     * Không hợp lệ:
     * http://example.atlassian.net
     * https://example.atlassian.net/path
     * https://example.atlassian.net?x=1
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

            String scheme =
                    uri.getScheme();

            /*
             * Chỉ HTTPS.
             */
            if (!"https".equalsIgnoreCase(scheme)) {

                throw new JiraClientException(
                        "Jira base URL phải sử dụng HTTPS");
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
             * không tạo thành "//rest/api/..."
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
     * Kiểm tra DNS trước khi request.
     *
     * Chặn:
     * - loopback
     * - private
     * - link-local
     * - multicast
     * - unspecified
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
             * Nếu một hostname resolve ra nhiều IP,
             * chỉ cần một IP không an toàn thì reject.
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
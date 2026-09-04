package vn.edu.cnpm.projectsupport.integration.github;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** JDK HTTP transport for the official GitHub REST API. */
public final class JdkGitHubHttpTransport implements GitHubHttpTransport {

    private static final String OFFICIAL_HOST = "api.github.com";
    private static final int MAX_RESPONSE_BYTES = 10 * 1024 * 1024;

    private final HttpClient httpClient;

    public JdkGitHubHttpTransport() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public GitHubHttpResponse get(
            String url,
            Map<String, String> headers,
            Duration timeout) throws IOException, InterruptedException {

        URI uri = URI.create(url);
        validateUri(uri);

        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .GET()
                .timeout(timeout)
                .header("Accept-Encoding", "identity");

        if (headers != null) {
            headers.forEach((name, value) -> {
                if (name != null && !name.isBlank()
                        && !name.equalsIgnoreCase("Host")
                        && !name.equalsIgnoreCase("Content-Length")
                        && !name.equalsIgnoreCase("Connection")
                        && value != null) {
                    builder.header(name, value);
                }
            });
        }

        HttpResponse<byte[]> response = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofByteArray());

        byte[] bodyBytes = response.body();
        if (bodyBytes.length > MAX_RESPONSE_BYTES) {
            throw new IOException("GitHub response exceeds the configured safety limit");
        }

        Map<String, String> responseHeaders = new LinkedHashMap<>();
        response.headers().map().forEach((name, values) -> {
            if (values != null && !values.isEmpty()) {
                responseHeaders.put(name.toLowerCase(Locale.ROOT), String.join(", ", values));
            }
        });

        return new GitHubHttpResponse(
                response.statusCode(),
                new String(bodyBytes, StandardCharsets.UTF_8),
                Map.copyOf(responseHeaders));
    }

    private static void validateUri(URI uri) {
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("GitHub URL must use HTTPS");
        }
        if (uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("GitHub URL contains unsupported URI components");
        }
        if (!OFFICIAL_HOST.equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Only the official GitHub API origin is allowed");
        }
    }
}

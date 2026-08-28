package vn.edu.cnpm.projectsupport.integration.jira;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.springframework.stereotype.Component;

@Component
public class JdkJiraHttpTransport implements JiraHttpTransport {

    private static final int HTTPS_DEFAULT_PORT = 443;

    private static final int MAX_RESPONSE_BYTES =
            10 * 1024 * 1024;

    private final SSLSocketFactory sslSocketFactory;

    public JdkJiraHttpTransport() {
        this.sslSocketFactory =
                (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    @Override
public JiraHttpResponse get(
        String url,
        Map<String, String> headers,
        java.time.Duration timeout)
        throws IOException, InterruptedException {

    URI uri = URI.create(url);

    validateUri(uri);

    InetAddress address =
            resolveAndValidateHost(uri.getHost());

    int port =
            uri.getPort() == -1
                    ? HTTPS_DEFAULT_PORT
                    : uri.getPort();

    int timeoutMillis =
            toTimeoutMillis(timeout);

    /*
     * Kết nối TCP TRỰC TIẾP tới IP đã được kiểm tra.
     *
     * Không kết nối tới hostname nữa.
     * Đây là điểm chống DNS rebinding quan trọng.
     */
    try (SSLSocket socket =
                 (SSLSocket) sslSocketFactory.createSocket()) {

        socket.setSoTimeout(timeoutMillis);

        socket.connect(
                new InetSocketAddress(address, port),
                timeoutMillis);

        configureTls(
                socket,
                uri.getHost());

        socket.startHandshake();

        sendRequest(
                socket,
                uri,
                headers,
                port);

        return readResponse(socket);

    } catch (GeneralSecurityException exception) {

        throw new IOException(
                "TLS connection tới Jira thất bại",
                exception);
    }
}

    /**
     * Chỉ chấp nhận HTTPS.
     *
     * Transport cho phép path/query vì Jira API cần chúng.
     * Việc kiểm tra base URL phải là HTTPS origin được thực hiện
     * thêm ở JiraRestClient.normalizeBaseUrl().
     */
    private void validateUri(URI uri) {

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException(
                    "Jira URL phải sử dụng HTTPS");
        }

        if (uri.getHost() == null
                || uri.getHost().isBlank()) {

            throw new IllegalArgumentException(
                    "Jira URL phải có hostname");
        }

        if (uri.getUserInfo() != null) {

            throw new IllegalArgumentException(
                    "Jira URL không được chứa user information");
        }

        if (uri.getFragment() != null) {

            throw new IllegalArgumentException(
                    "Jira URL không được chứa fragment");
        }

        int port = uri.getPort();

        if (port != -1
                && (port < 1 || port > 65535)) {

            throw new IllegalArgumentException(
                    "Jira URL có port không hợp lệ");
        }
    }

    /**
     * Resolve DNS đúng một lần.
     *
     * Tất cả IP trả về đều phải an toàn.
     *
     * Sau khi chọn IP, socket sẽ connect trực tiếp tới IP này,
     * không resolve hostname lần thứ hai.
     */
    private InetAddress resolveAndValidateHost(
            String host) {

        try {

            InetAddress[] addresses =
                    InetAddress.getAllByName(host);

            if (addresses.length == 0) {

                throw new IllegalArgumentException(
                        "Jira host không phân giải được");
            }

            /*
             * Nếu BẤT KỲ record nào nguy hiểm thì reject toàn bộ
             * hostname.
             *
             * Tránh trường hợp DNS trả:
             *
             *   public IP
             *   +
             *   private IP
             */
            for (InetAddress address : addresses) {

                if (isUnsafeAddress(address)) {

                    throw new IllegalArgumentException(
                            "Jira host trỏ tới địa chỉ mạng không được phép");
                }
            }

            /*
             * Chọn IP đã được kiểm tra.
             *
             * Không gọi DNS lại sau bước này.
             */
            return addresses[0];

        } catch (UnknownHostException exception) {

            throw new IllegalArgumentException(
                    "Không thể phân giải Jira host",
                    exception);
        }
    }

    /**
     * Chặn các địa chỉ có thể bị lợi dụng cho SSRF.
     */
    private boolean isUnsafeAddress(
            InetAddress address) {

        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address);
    }

    /**
     * IPv6 Unique Local Address:
     *
     * fc00::/7
     */
    private boolean isUniqueLocalIpv6(
            InetAddress address) {

        byte[] bytes =
                address.getAddress();

        return bytes.length == 16
                && (bytes[0] & 0xFE) == 0xFC;
    }

    /**
     * TLS vẫn xác thực hostname Jira.
     *
     * TCP:
     *
     *     IP đã pin
     *
     * TLS/SNI:
     *
     *     jira.example.com
     *
     * Như vậy vừa chống DNS rebinding vừa không phá
     * certificate/SNI của Jira Cloud.
     */
    private void configureTls(
            SSLSocket socket,
            String hostname)
            throws GeneralSecurityException {

        SSLParameters parameters =
                socket.getSSLParameters();

        parameters.setEndpointIdentificationAlgorithm(
                "HTTPS");

        parameters.setServerNames(
                List.of(new SNIHostName(hostname)));

        socket.setSSLParameters(parameters);
    }

    private void sendRequest(
            SSLSocket socket,
            URI uri,
            Map<String, String> headers,
            int port)
            throws IOException {

        OutputStream output =
                socket.getOutputStream();

        String hostHeader =
                uri.getHost();

        if (port != HTTPS_DEFAULT_PORT) {
            hostHeader += ":" + port;
        }

        StringBuilder request =
                new StringBuilder();

        request.append("GET ")
                .append(buildRequestTarget(uri))
                .append(" HTTP/1.1\r\n");

        request.append("Host: ")
                .append(hostHeader)
                .append("\r\n");

        /*
         * Không nhận gzip để tránh phải xử lý compression
         * ở transport.
         */
        request.append("Accept-Encoding: identity\r\n");

        /*
         * Không cho phép client gửi Connection: close từ ngoài
         * làm thay đổi logic đọc response.
         */
        request.append("Connection: close\r\n");

        if (headers != null) {

            for (Map.Entry<String, String> entry :
                    headers.entrySet()) {

                String name =
                        entry.getKey();

                if (name == null
                        || name.isBlank()) {
                    continue;
                }

                /*
                 * Không cho caller override các header
                 * liên quan tới routing.
                 */
                if (name.equalsIgnoreCase("Host")
                        || name.equalsIgnoreCase("Connection")
                        || name.equalsIgnoreCase("Content-Length")
                        || name.equalsIgnoreCase("Transfer-Encoding")
                        || name.equalsIgnoreCase("Accept-Encoding")) {
                    continue;
                }

                request.append(name)
                        .append(": ")
                        .append(entry.getValue())
                        .append("\r\n");
            }
        }

        request.append("\r\n");

        output.write(
                request.toString()
                        .getBytes(StandardCharsets.ISO_8859_1));

        output.flush();
    }

    private String buildRequestTarget(
            URI uri) {

        String path =
                uri.getRawPath();

        if (path == null
                || path.isBlank()) {

            path = "/";
        }

        String query =
                uri.getRawQuery();

        if (query != null
                && !query.isBlank()) {

            path += "?" + query;
        }

        return path;
    }

    /**
     * Đọc HTTP response.
     *
     * Hỗ trợ:
     * - Content-Length
     * - Transfer-Encoding: chunked
     * - connection close
     */
    private JiraHttpResponse readResponse(
            SSLSocket socket)
            throws IOException {

        InputStream input =
                socket.getInputStream();

        String statusLine =
                readLine(input);

        if (statusLine == null
                || statusLine.isBlank()) {

            throw new IOException(
                    "Jira không trả về HTTP response");
        }

        int status =
                parseStatusCode(statusLine);

        Map<String, String> responseHeaders =
                readHeaders(input);

        byte[] body;

        String transferEncoding =
                responseHeaders.get(
                        "transfer-encoding");

        String contentLength =
                responseHeaders.get(
                        "content-length");

        if (transferEncoding != null
                && transferEncoding
                        .toLowerCase(Locale.ROOT)
                        .contains("chunked")) {

            body =
                    readChunkedBody(input);

        } else if (contentLength != null) {

            body =
                    readFixedBody(
                            input,
                            parseContentLength(contentLength));

        } else {

            body =
                    readUntilClose(input);
        }

        return new JiraHttpResponse(
                status,
                new String(
                        body,
                        StandardCharsets.UTF_8),
                responseHeaders);
    }

    private int parseStatusCode(
            String statusLine)
            throws IOException {

        String[] parts =
                statusLine.split("\\s+", 3);

        if (parts.length < 2) {

            throw new IOException(
                    "HTTP status line không hợp lệ");
        }

        try {

            return Integer.parseInt(parts[1]);

        } catch (NumberFormatException exception) {

            throw new IOException(
                    "HTTP status code không hợp lệ",
                    exception);
        }
    }

    private Map<String, String> readHeaders(
            InputStream input)
            throws IOException {

        Map<String, String> headers =
                new HashMap<>();

        while (true) {

            String line =
                    readLine(input);

            if (line == null) {

                throw new IOException(
                        "HTTP response kết thúc trước headers");
            }

            if (line.isEmpty()) {
                break;
            }

            int separator =
                    line.indexOf(':');

            if (separator <= 0) {
                continue;
            }

            String name =
                    line.substring(
                            0,
                            separator)
                            .trim()
                            .toLowerCase(Locale.ROOT);

            String value =
                    line.substring(separator + 1)
                            .trim();

            headers.merge(
                    name,
                    value,
                    (left, right) ->
                            left + "," + right);
        }

        return headers;
    }

    private byte[] readFixedBody(
            InputStream input,
            long length)
            throws IOException {

        if (length < 0
                || length > MAX_RESPONSE_BYTES) {

            throw new IOException(
                    "Jira response quá lớn");
        }

        ByteArrayOutputStream output =
                new ByteArrayOutputStream(
                        (int) length);

        byte[] buffer =
                new byte[8192];

        long remaining =
                length;

        while (remaining > 0) {

            int read =
                    input.read(
                            buffer,
                            0,
                            (int) Math.min(
                                    buffer.length,
                                    remaining));

            if (read == -1) {

                throw new IOException(
                        "Jira response bị cắt trước Content-Length");
            }

            output.write(
                    buffer,
                    0,
                    read);

            remaining -= read;
        }

        return output.toByteArray();
    }

    private byte[] readUntilClose(
            InputStream input)
            throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        byte[] buffer =
                new byte[8192];

        int total = 0;

        int read;

        while ((read = input.read(buffer)) != -1) {

            total += read;

            if (total > MAX_RESPONSE_BYTES) {

                throw new IOException(
                        "Jira response quá lớn");
            }

            output.write(
                    buffer,
                    0,
                    read);
        }

        return output.toByteArray();
    }

    private byte[] readChunkedBody(
            InputStream input)
            throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        while (true) {

            String sizeLine =
                    readLine(input);

            if (sizeLine == null) {

                throw new IOException(
                        "Chunked response không hợp lệ");
            }

            int semicolon =
                    sizeLine.indexOf(';');

            String sizeText =
                    semicolon >= 0
                            ? sizeLine.substring(
                                    0,
                                    semicolon)
                            : sizeLine;

            long chunkSize;

            try {

                chunkSize =
                        Long.parseLong(
                                sizeText.trim(),
                                16);

            } catch (NumberFormatException exception) {

                throw new IOException(
                        "Chunk size không hợp lệ",
                        exception);
            }

            if (chunkSize == 0) {

                /*
                 * Đọc trailing headers.
                 */
                while (true) {

                    String trailer =
                            readLine(input);

                    if (trailer == null
                            || trailer.isEmpty()) {
                        break;
                    }
                }

                break;
            }

            if (chunkSize > MAX_RESPONSE_BYTES
                    || output.size()
                    + chunkSize
                    > MAX_RESPONSE_BYTES) {

                throw new IOException(
                        "Jira response quá lớn");
            }

            byte[] chunk =
                    readExact(
                            input,
                            chunkSize);

            output.write(chunk);

            /*
             * CRLF sau chunk.
             */
            int cr =
                    input.read();

            int lf =
                    input.read();

            if (cr != '\r'
                    || lf != '\n') {

                throw new IOException(
                        "Chunked response thiếu CRLF");
            }
        }

        return output.toByteArray();
    }

    private byte[] readExact(
            InputStream input,
            long length)
            throws IOException {

        if (length > Integer.MAX_VALUE) {

            throw new IOException(
                    "Response body quá lớn");
        }

        byte[] result =
                new byte[(int) length];

        int offset = 0;

        while (offset < result.length) {

            int read =
                    input.read(
                            result,
                            offset,
                            result.length - offset);

            if (read == -1) {

                throw new IOException(
                        "Response kết thúc bất thường");
            }

            offset += read;
        }

        return result;
    }

    private long parseContentLength(
            String value)
            throws IOException {

        try {

            long length =
                    Long.parseLong(value.trim());

            if (length < 0
                    || length > MAX_RESPONSE_BYTES) {

                throw new IOException(
                        "Content-Length không hợp lệ");
            }

            return length;

        } catch (NumberFormatException exception) {

            throw new IOException(
                    "Content-Length không hợp lệ",
                    exception);
        }
    }

    private String readLine(
            InputStream input)
            throws IOException {

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        int previous = -1;

        while (true) {

            int current =
                    input.read();

            if (current == -1) {

                if (output.size() == 0) {
                    return null;
                }

                return output.toString(
                        StandardCharsets.ISO_8859_1);
            }

            if (previous == '\r'
                    && current == '\n') {

                byte[] bytes =
                        output.toByteArray();

                return new String(
                        bytes,
                        0,
                        bytes.length - 1,
                        StandardCharsets.ISO_8859_1);
            }

            output.write(current);

            previous = current;

            /*
             * Chặn HTTP header/line bất thường.
             */
            if (output.size() > 64 * 1024) {

                throw new IOException(
                        "HTTP line quá dài");
            }
        }
    }

    private int toTimeoutMillis(
            java.time.Duration timeout) {

        if (timeout == null
                || timeout.isNegative()
                || timeout.isZero()) {

            return 10_000;
        }

        long millis =
                timeout.toMillis();

        if (millis <= 0) {
            return 1;
        }

        return (int) Math.min(
                millis,
                Integer.MAX_VALUE);
    }
}
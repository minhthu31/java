package vn.edu.cnpm.projectsupport.security;

import java.util.regex.Pattern;

/** Removes credentials from messages before they are persisted or returned by an API. */
public final class SensitiveDataSanitizer {

    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final String REDACTED = "[REDACTED]";

    private static final Pattern AUTHORIZATION_HEADER = Pattern.compile(
            "(?i)\\bauthorization\\s*[:=]\\s*(?:bearer|basic)\\s+[^\\s,;]+"
    );
    private static final Pattern AUTH_SCHEME = Pattern.compile(
            "(?i)\\b(?:bearer|basic)\\s+[^\\s,;]+"
    );
    private static final Pattern NAMED_SECRET = Pattern.compile(
            "(?i)\\b(password|passphrase|token|api[-_ ]?key|secret|credentials?"
                    + "|client[-_ ]?secret|access[-_ ]?token|refresh[-_ ]?token)\\b"
                    + "\\s*[:=]\\s*(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;&]+)"
    );
    private static final Pattern URL_CREDENTIAL = Pattern.compile(
            "(?i)(https?://[^\\s/:]+:)[^\\s@]+@"
    );
    private static final Pattern GITHUB_TOKEN = Pattern.compile(
            "(?i)\\b(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,})\\b"
    );
    private static final Pattern ATLASSIAN_TOKEN = Pattern.compile(
            "\\bATATT[A-Za-z0-9_-]{20,}\\b"
    );
    private static final Pattern JWT = Pattern.compile(
            "\\b[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}\\b"
    );

    private SensitiveDataSanitizer() {
    }

    public static String sanitize(Throwable exception, String fallbackMessage) {
        String message = exception == null ? null : exception.getMessage();
        return sanitize(message, fallbackMessage);
    }

    public static String sanitize(String message, String fallbackMessage) {
        String fallback = fallbackMessage == null || fallbackMessage.isBlank()
                ? "External provider request failed"
                : fallbackMessage;
        if (message == null || message.isBlank()) {
            return fallback;
        }

        String sanitized = AUTHORIZATION_HEADER.matcher(message)
                .replaceAll("Authorization: " + REDACTED);
        sanitized = AUTH_SCHEME.matcher(sanitized)
                .replaceAll(REDACTED);
        sanitized = NAMED_SECRET.matcher(sanitized)
                .replaceAll("$1=" + REDACTED);
        sanitized = URL_CREDENTIAL.matcher(sanitized)
                .replaceAll("$1" + REDACTED + "@");
        sanitized = GITHUB_TOKEN.matcher(sanitized)
                .replaceAll(REDACTED);
        sanitized = ATLASSIAN_TOKEN.matcher(sanitized)
                .replaceAll(REDACTED);
        sanitized = JWT.matcher(sanitized)
                .replaceAll(REDACTED);

        return sanitized.substring(0, Math.min(sanitized.length(), MAX_MESSAGE_LENGTH));
    }
}

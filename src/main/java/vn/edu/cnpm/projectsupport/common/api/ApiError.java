package vn.edu.cnpm.projectsupport.common.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        String code,
        String message,
        String correlationId,
        Map<String, String> fieldErrors,
        Instant timestamp) {
}

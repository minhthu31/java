package vn.edu.cnpm.projectsupport.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String message,
        String correlationId,
        Map<String, String> fieldErrors,
        Instant timestamp,
        Boolean retryable,
        Long retryAfterSeconds) {

    public ApiError(
            String code,
            String message,
            String correlationId,
            Map<String, String> fieldErrors,
            Instant timestamp) {

        this(
                code,
                message,
                correlationId,
                fieldErrors,
                timestamp,
                null,
                null);
    }

    public ApiError(
            String code,
            String message,
            String correlationId,
            Map<String, String> fieldErrors,
            Instant timestamp,
            Boolean retryable) {

        this(
                code,
                message,
                correlationId,
                fieldErrors,
                timestamp,
                retryable,
                null);
    }
}
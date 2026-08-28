package vn.edu.cnpm.projectsupport.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import vn.edu.cnpm.projectsupport.common.api.ApiError;
import vn.edu.cnpm.projectsupport.integration.jira.exception.JiraApiException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu đầu vào không hợp lệ", fields, request);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException exception, WebRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler({AccessDeniedException.class, ForbiddenGroupScopeException.class})
    public ResponseEntity<ApiError> handleAccessDenied(Exception exception, WebRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Bạn không có quyền thực hiện thao tác này", Map.of(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(InvalidCredentialsException exception, WebRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(ResourceInUseException.class)
    public ResponseEntity<ApiError> handleResourceInUse(ResourceInUseException exception, WebRequest request) {
        return error(HttpStatus.CONFLICT, "RESOURCE_IN_USE", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidStatusTransition(InvalidStatusTransitionException exception, WebRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STATUS_TRANSITION", exception.getMessage(), Map.of(), request);
    }

    @ExceptionHandler(AssigneeOutsideGroupException.class)
    public ResponseEntity<ApiError> handleAssigneeOutsideGroup(AssigneeOutsideGroupException exception, WebRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "ASSIGNEE_OUTSIDE_GROUP",exception.getMessage(), Map.of(),request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception, WebRequest request) {
        return error(HttpStatus.BAD_REQUEST,"VALIDATION_FAILED",exception.getMessage(),Map.of(),request);
    }

    @ExceptionHandler(JiraApiException.class)
    public ResponseEntity<ApiError> handleJiraApiException(JiraApiException exception,WebRequest request) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if (exception.getRetryAfterSeconds() != null) {
            response.header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()));
        }
        return response.body(new ApiError(exception.getErrorCode(), exception.getMessage(), exception.getCorrelationId(), Map.of(), Instant.now(), exception.isRetryable()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, WebRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Hệ thống gặp lỗi ngoài dự kiến", Map.of(), request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message, Map<String, String> fields, WebRequest request) {
        return ResponseEntity.status(status).body(new ApiError(code, message, correlationId(request), fields, Instant.now()));
    }

    private String correlationId(WebRequest request) {
        String header = request.getHeader("X-Correlation-ID");
        if (header != null && !header.isBlank()) {
            return header;
        }
        String current = MDC.get("correlationId");
        return current == null || current.isBlank() ? UUID.randomUUID().toString() : current;
    }
}

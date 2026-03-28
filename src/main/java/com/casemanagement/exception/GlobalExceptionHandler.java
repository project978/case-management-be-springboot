package com.casemanagement.exception;

import com.casemanagement.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ─── Timestamp helper ────────────────────────────────────────────────────

    private Map<String, Object> buildErrorBody(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }

    // =========================================================================
    // 1. CUSTOM APPLICATION EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("[404] Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("[400] Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomAccessDenied(AccessDeniedException ex) {
        log.warn("[403] Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // =========================================================================
    // 2. SPRING SECURITY EXCEPTIONS
    // =========================================================================

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        log.warn("[403] Spring security access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: you do not have permission to perform this action"));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[401] Bad credentials: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password"));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiResponse<Void>> handleDisabledUser(DisabledException ex) {
        log.warn("[401] Disabled account: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Your account has been disabled. Please contact support."));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLockedUser(LockedException ex) {
        log.warn("[401] Locked account: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Your account has been locked. Please contact support."));
    }

    // =========================================================================
    // 3. VALIDATION EXCEPTIONS
    // =========================================================================

    /** @RequestBody @Valid failures — field-level errors */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> fieldErrors = new ArrayList<>();
        List<String> globalErrors = new ArrayList<>();

        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(fe.getField() + ": " + fe.getDefaultMessage());
        }
        for (ObjectError oe : ex.getBindingResult().getGlobalErrors()) {
            globalErrors.add(oe.getObjectName() + ": " + oe.getDefaultMessage());
        }

        Map<String, Object> errors = new LinkedHashMap<>();
        if (!fieldErrors.isEmpty())  errors.put("fieldErrors", fieldErrors);
        if (!globalErrors.isEmpty()) errors.put("globalErrors", globalErrors);

        log.warn("[400] Validation failed: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    /** @RequestParam / @PathVariable @Valid failures */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolation(
            ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations()
                .stream()
                .map(cv -> {
                    String path = cv.getPropertyPath().toString();
                    // strip method name prefix (e.g. "getUser.id" → "id")
                    String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    return field + ": " + cv.getMessage();
                })
                .collect(Collectors.toList());

        log.warn("[400] Constraint violations: {}", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<List<String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .build());
    }

    // =========================================================================
    // 4. HTTP / REQUEST EXCEPTIONS
    // =========================================================================

    /** Wrong HTTP method (e.g. GET instead of POST) */
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String supported = ex.getSupportedHttpMethods() != null
                ? ex.getSupportedHttpMethods().toString()
                : "N/A";
        String message = String.format(
                "HTTP method '%s' is not supported for this endpoint. Supported: %s",
                ex.getMethod(), supported);

        log.warn("[405] Method not allowed: {}", message);

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(message));
    }

    /** Wrong Content-Type header */
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = String.format(
                "Media type '%s' is not supported. Supported types: %s",
                ex.getContentType(),
                ex.getSupportedMediaTypes());

        log.warn("[415] Unsupported media type: {}", message);

        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(message));
    }

    /** Malformed / unreadable JSON body */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        log.warn("[400] Unreadable HTTP message: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Malformed JSON request body. Please check your request format."));
    }

    /** Missing required @RequestParam */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = "Required request parameter '" + ex.getParameterName()
                + "' of type '" + ex.getParameterType() + "' is missing";
        log.warn("[400] Missing request parameter: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /** Missing multipart file part */
    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(
            MissingServletRequestPartException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = "Required request part '" + ex.getRequestPartName() + "' is missing";
        log.warn("[400] Missing request part: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /** URL not found — 404 for unknown endpoints */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        String message = "No endpoint found for: " + ex.getHttpMethod() + " " + ex.getRequestURL();
        log.warn("[404] No handler found: {}", message);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(message));
    }

    /** Wrong type for path variable or request param (e.g. string where int expected) */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        String expected = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String message = String.format(
                "Parameter '%s' should be of type '%s' but received: '%s'",
                ex.getName(), expected, ex.getValue());

        log.warn("[400] Type mismatch: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /** Missing required header */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(
            MissingRequestHeaderException ex) {

        String message = "Required request header '" + ex.getHeaderName() + "' is missing";
        log.warn("[400] Missing header: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // 5. FILE UPLOAD EXCEPTIONS
    // =========================================================================

    /** File too large — though we set unlimited in yml, kept as safety net */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        if (ex instanceof MaxUploadSizeExceededException) {
            log.warn("[413] File too large: {}", ex.getMessage());

            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiResponse.error(
                            "Uploaded file exceeds the maximum allowed size. Please upload a smaller file."));
        }

        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    // =========================================================================
    // 6. DATABASE / PERSISTENCE EXCEPTIONS
    // =========================================================================

    /** Unique constraint violation, FK violations, NOT NULL violations, etc. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        log.error("[409] Data integrity violation: {}", ex.getMostSpecificCause().getMessage());

        String message = "Database constraint violated.";
        String cause = ex.getMostSpecificCause().getMessage();

        // Provide user-friendly messages for common constraints
        if (cause != null) {
            if (cause.contains("users_email_key") || cause.contains("unique") && cause.contains("email")) {
                message = "An account with this email address already exists.";
            } else if (cause.contains("not-null") || cause.contains("null value in column")) {
                message = "A required field is missing. Please check your request.";
            } else if (cause.contains("foreign key") || cause.contains("violates foreign key")) {
                message = "The referenced resource does not exist.";
            }
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(message));
    }

    // =========================================================================
    // 7. FALLBACK — catch-all for any unhandled exception
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAllUncaughtExceptions(Exception ex) {
        log.error("[500] Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. Please try again later or contact support."));
    }
}

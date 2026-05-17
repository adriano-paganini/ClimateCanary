package at.qe.skeleton.common;

import at.qe.skeleton.common.exceptions.ConflictException;
import at.qe.skeleton.common.exceptions.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles NOT FOUND (404) issues
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            NotFoundException ex,
            HttpServletRequest request)
    {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", ex, request);
    }

    /**
     * Handles CONFLICT (409) issues
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request)
    {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex, request);
    }

    /**
     * Handles BAD REQUEST (400) issues
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            RuntimeException ex,
            HttpServletRequest request)
    {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex, request);
    }

    /**
     * Handles Jakarta VALIDATION ERRORS
     * @param ex
     * @return
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(
            MethodArgumentNotValidException ex)
    {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> errors.put(err.getField(), err.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    /**
     * Handles FORBIDDEN (403) issues
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(
            Exception ex,
            HttpServletRequest request)
    {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", ex, request);
    }

    /**
     * Handles UNAUTHORIZED (401) issues
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(
            Exception ex,
            HttpServletRequest request)
    {
        return build(HttpStatus.FORBIDDEN, "UNAUTHORIZED", ex, request);
    }

    /**
     * Handles FALLBACK issues
     * @param ex
     * @param request
     * @return
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request)
    {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", ex, request);

    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String code,
            Exception ex,
            HttpServletRequest request
    ) {
        return new ResponseEntity<>(
                new ApiErrorResponse(
                        Instant.now(),
                        status.value(),
                        code,
                        ex.getMessage(),
                        request.getMethod() + " " + request.getRequestURI()),
                status
        );
    }
}



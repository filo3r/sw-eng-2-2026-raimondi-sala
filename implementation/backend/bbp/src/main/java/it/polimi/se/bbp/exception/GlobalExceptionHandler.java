package it.polimi.se.bbp.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import it.polimi.se.bbp.exception.mapbox.*;
import it.polimi.se.bbp.exception.obstacle.ObstacleNotFoundException;
import it.polimi.se.bbp.exception.obstacle.ObstacleTooFarException;
import it.polimi.se.bbp.exception.openmeteo.*;
import it.polimi.se.bbp.exception.user.UserAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for the application.
 * Returns consistent error responses with request ID, user ID logging, and path tracking.
 * Differentiates logging: WARN for client errors (4xx), ERROR for server errors (5xx).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========================================
    // HELPER METHODS
    // ========================================

    /**
     * Creates standard error response with all required fields.
     * @param status HTTP status code
     * @param error short error description
     * @param message detailed user-friendly message
     * @param request HTTP request for extracting path
     * @return map containing error response structure
     */
    private Map<String, Object> createErrorResponse(HttpStatus status, String error,
                                                    String message, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", request.getRequestURI());
        response.put("requestId", UUID.randomUUID().toString());
        return response;
    }

    /**
     * Logs warning for client errors (4xx).
     * Does NOT include stack trace as these are expected user errors.
     * @param requestId unique request identifier
     * @param exceptionName name of the exception class
     * @param message error message
     * @param path request path
     */
    private void logWarn(String requestId, String exceptionName, String message, String path) {
        Long userId = getAuthenticatedUserId();
        String userInfo = userId != null ? "User ID: " + userId : "Anonymous";
        log.warn("[{}] {}: {} | {} | Path: {}",
                requestId, exceptionName, message, userInfo, path);
    }

    /**
     * Logs error for server errors (5xx).
     * Includes full stack trace for debugging.
     * @param requestId unique request identifier
     * @param exceptionName name of the exception class
     * @param message error message
     * @param path request path
     * @param ex exception for stack trace
     */
    private void logError(String requestId, String exceptionName,
                          String message, String path, Throwable ex) {
        Long userId = getAuthenticatedUserId();
        String userInfo = userId != null ? "User ID: " + userId : "Anonymous";
        log.error("[{}] {}: {} | {} | Path: {}",
                requestId, exceptionName, message, userInfo, path, ex);
    }

    /**
     * Safely retrieves authenticated user ID for logging.
     * Returns null if not authenticated (public endpoints).
     * Never throws to avoid breaking exception handler.
     * @return user ID or null if not authenticated
     */
    private Long getAuthenticatedUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Long) {
                    return (Long) principal;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ========================================
    // CLIENT ERRORS - 400 BAD REQUEST
    // ========================================

    /**
     * Handles invalid address from Mapbox geocoding.
     * @param ex exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidAddressException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAddress(
            InvalidAddressException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Address",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "InvalidAddressException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles invalid coordinates from Mapbox reverse geocoding.
     * @param ex exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidCoordinateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCoordinate(
            InvalidCoordinateException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Coordinate",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "InvalidCoordinateException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles invalid route from Mapbox routing.
     * @param ex exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(InvalidRouteException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRoute(
            InvalidRouteException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Route",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "InvalidRouteException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles obstacle too far from bike path.
     * @param ex exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(ObstacleTooFarException.class)
    public ResponseEntity<Map<String, Object>> handleObstacleTooFar(
            ObstacleTooFarException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Obstacle Too Far",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "ObstacleTooFarException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles weather data not available for requested time period.
     * @param ex exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(WeatherDataNotAvailableException.class)
    public ResponseEntity<Map<String, Object>> handleWeatherDataNotAvailable(
            WeatherDataNotAvailableException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Weather Data Not Available",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "WeatherDataNotAvailableException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles illegal arguments (invalid pagination, business rule violations).
     * @param ex exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "IllegalArgumentException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles validation errors from @Valid annotations on request DTOs.
     * @param ex validation exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST and field-specific errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);
        response.put("path", request.getRequestURI());
        response.put("requestId", UUID.randomUUID().toString());
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "MethodArgumentNotValidException",
                "Validation failed for " + errors.size() + " field(s)", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles constraint violations at entity level.
     * @param ex constraint violation exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST and constraint errors
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(
            jakarta.validation.ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        });
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);
        response.put("path", request.getRequestURI());
        response.put("requestId", UUID.randomUUID().toString());
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "ConstraintViolationException",
                "Constraint violation for " + errors.size() + " field(s)", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles malformed or invalid JSON in request body.
     * @param ex message not readable exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON",
                "Request body contains invalid JSON",
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "HttpMessageNotReadableException",
                "Invalid JSON in request body", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles missing required request parameters.
     * @param ex missing parameter exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing Parameter",
                message,
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "MissingServletRequestParameterException", message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handles type mismatch in path variables or request parameters.
     * @param ex type mismatch exception
     * @param request HTTP request
     * @return error response with 400 BAD REQUEST
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(),
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        Map<String, Object> response = createErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Type Mismatch",
                message,
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "MethodArgumentTypeMismatchException", message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ========================================
    // AUTHENTICATION ERRORS - 401 UNAUTHORIZED
    // ========================================

    /**
     * Handles bad credentials during authentication.
     * @param ex bad credentials exception
     * @param request HTTP request
     * @return error response with 401 UNAUTHORIZED
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Authentication Failed",
                "Invalid email or password",
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "BadCredentialsException", "Failed login attempt", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles expired JWT token.
     * @param ex expired JWT exception
     * @param request HTTP request
     * @return error response with 401 UNAUTHORIZED
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwtException(
            ExpiredJwtException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Token Expired",
                "JWT token has expired. Please login again.",
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "ExpiredJwtException", "Expired JWT token", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles invalid JWT signature.
     * @param ex signature exception
     * @param request HTTP request
     * @return error response with 401 UNAUTHORIZED
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureException(
            SignatureException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Invalid Token",
                "JWT signature is invalid",
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "SignatureException", "Invalid JWT signature", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handles user not found.
     * @param ex user not found exception
     * @param request HTTP request
     * @return error response with 404 NOT FOUND
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UsernameNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.NOT_FOUND,
                "User Not Found",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "UsernameNotFoundException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ========================================
    // AUTHORIZATION ERRORS - 403 FORBIDDEN
    // ========================================

    /**
     * Handles access denied (user lacks permission).
     * @param ex access denied exception
     * @param request HTTP request
     * @return error response with 403 FORBIDDEN
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "AccessDeniedException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ========================================
    // NOT FOUND ERRORS - 404
    // ========================================

    /**
     * Handles entity not found.
     * @param ex entity not found exception
     * @param request HTTP request
     * @return error response with 404 NOT FOUND
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.NOT_FOUND,
                "Not Found",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "EntityNotFoundException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handles obstacle not found or not belonging to bike path.
     * @param ex obstacle not found exception
     * @param request HTTP request
     * @return error response with 404 NOT FOUND
     */
    @ExceptionHandler(ObstacleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleObstacleNotFound(
            ObstacleNotFoundException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.NOT_FOUND,
                "Obstacle Not Found",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "ObstacleNotFoundException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // ========================================
    // CONFLICT ERRORS - 409
    // ========================================

    /**
     * Handles user already exists (duplicate username or email).
     * @param ex user already exists exception
     * @param request HTTP request
     * @return error response with 409 CONFLICT
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(
            UserAlreadyExistsException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.CONFLICT,
                "User Already Exists",
                ex.getMessage(),
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "UserAlreadyExistsException", ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles optimistic lock exceptions (concurrent updates).
     * @param ex optimistic lock exception
     * @param request HTTP request
     * @return error response with 409 CONFLICT
     */
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLockException(
            OptimisticLockException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                "The resource was modified by another user. Please refresh and try again.",
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "OptimisticLockException",
                "Concurrent modification detected", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handles database integrity constraint violations.
     * Safety net for race conditions in unique constraints.
     * @param ex data integrity violation exception
     * @param request HTTP request
     * @return error response with 409 CONFLICT
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.CONFLICT,
                "Conflict",
                "A unique constraint has been violated. Please check your input.",
                request
        );
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "DataIntegrityViolationException",
                "Database constraint violation", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ========================================
    // METHOD NOT ALLOWED - 405
    // ========================================

    /**
     * Handles unsupported HTTP methods on endpoints.
     * @param ex method not supported exception
     * @param request HTTP request
     * @return error response with 405 METHOD NOT ALLOWED
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String message = String.format("HTTP method '%s' is not supported for this endpoint",
                ex.getMethod());
        Map<String, Object> response = createErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                message,
                request
        );
        response.put("supportedMethods", ex.getSupportedHttpMethods());
        String requestId = (String) response.get("requestId");
        logWarn(requestId, "HttpRequestMethodNotSupportedException", message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    // ========================================
    // RATE LIMIT ERRORS - 429
    // ========================================

    /**
     * Handles Mapbox API rate limit.
     * @param ex rate limit exception
     * @param request HTTP request
     * @return error response with 429 TOO MANY REQUESTS
     */
    @ExceptionHandler(MapboxRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleMapboxRateLimit(
            MapboxRateLimitException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                "The mapping service is experiencing high traffic. Please try again in a few moments.",
                request
        );
        String requestId = (String) response.get("requestId");
        logError(requestId, "MapboxRateLimitException", ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    /**
     * Handles Open-Meteo API rate limit.
     * @param ex rate limit exception
     * @param request HTTP request
     * @return error response with 429 TOO MANY REQUESTS
     */
    @ExceptionHandler(OpenMeteoRateLimitException.class)
    public ResponseEntity<Map<String, Object>> handleOpenMeteoRateLimit(
            OpenMeteoRateLimitException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                "The weather service is experiencing high traffic. Please try again in a few moments.",
                request
        );
        String requestId = (String) response.get("requestId");
        logError(requestId, "OpenMeteoRateLimitException", ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
    }

    // ========================================
    // SERVICE UNAVAILABLE ERRORS - 503
    // ========================================

    /**
     * Handles Mapbox API errors or unavailability.
     * @param ex API exception
     * @param request HTTP request
     * @return error response with 503 SERVICE UNAVAILABLE
     */
    @ExceptionHandler(MapboxApiException.class)
    public ResponseEntity<Map<String, Object>> handleMapboxApiException(
            MapboxApiException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "The mapping service is temporarily unavailable. Please try again later.",
                request
        );
        String requestId = (String) response.get("requestId");
        logError(requestId, "MapboxApiException", ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles Mapbox timeout.
     * @param ex timeout exception
     * @param request HTTP request
     * @return error response with 503 SERVICE UNAVAILABLE
     */
    @ExceptionHandler(MapboxTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleMapboxTimeout(
            MapboxTimeoutException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "The mapping service request timed out. Please try again.",
                request
        );
        String requestId = (String) response.get("requestId");
        logError(requestId, "MapboxTimeoutException", ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles Open-Meteo API errors or unavailability.
     * @param ex API exception
     * @param request HTTP request
     * @return error response with 503 SERVICE UNAVAILABLE
     */
    @ExceptionHandler(OpenMeteoApiException.class)
    public ResponseEntity<Map<String, Object>> handleOpenMeteoApiException(
            OpenMeteoApiException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "The weather service is temporarily unavailable. Please try again later.",
                request
        );
        String requestId = (String) response.get("requestId");
        logError(requestId, "OpenMeteoApiException", ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    /**
     * Handles Open-Meteo timeout.
     * @param ex timeout exception
     * @param request HTTP request
     * @return error response with 503 SERVICE UNAVAILABLE
     */
    @ExceptionHandler(OpenMeteoTimeoutException.class)
    public ResponseEntity<Map<String, Object>> handleOpenMeteoTimeout(
            OpenMeteoTimeoutException ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                "The weather service request timed out. Please try again.",
                request
        );
        String requestId = (String) response.get("requestId");
        logError(requestId, "OpenMeteoTimeoutException", ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    // ========================================
    // CATCH-ALL - 500 INTERNAL SERVER ERROR
    // ========================================

    /**
     * Handles all unhandled exceptions.
     * Final safety net for unexpected errors.
     * Always logs with ERROR level and full stack trace.
     * @param ex exception
     * @param request HTTP request
     * @return error response with 500 INTERNAL SERVER ERROR
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, HttpServletRequest request) {
        Map<String, Object> response = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request
        );
        String requestId = (String) response.get("requestId");
        logError(requestId, ex.getClass().getSimpleName(),
                "Unexpected error: " + ex.getMessage(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

}
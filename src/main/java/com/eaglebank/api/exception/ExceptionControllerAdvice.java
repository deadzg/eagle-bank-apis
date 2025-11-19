package com.eaglebank.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application using Spring's @RestControllerAdvice.
 * This class catches exceptions thrown by controllers across the application
 * and formats the response according to API standards.
 */
@RestControllerAdvice
public class ExceptionControllerAdvice {


    // Helper record for a standard error response structure
    private record ErrorResponse(
            String timestamp,
            int status,
            String error,
            String message,
            String path
    ) {}

    /** Maps to 422 UNPROCESSABLE ENTITY - Used when account has insufficient funds. */
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public static class InsufficientFundsException extends RuntimeException {
        public InsufficientFundsException(String message) {
            super(message);
        }
    }
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({ResourceNotFoundException.class})
    public Map<String, Object> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("statuscode", HttpStatus.NOT_FOUND.value());
        response.put("errormessage", ex.getMessage());
        return response;
    }


    /** Maps to 403 FORBIDDEN - Used when account is not associated with the userId. */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class ForbiddenOperationException extends RuntimeException {
        public ForbiddenOperationException(String message) {
            super(message);
        }
    }

    /**
     * Handles validation errors (e.g., failed @Email or @Pattern checks) and returns a 400 Bad Request.
     * Returns the standardized error response including specific field errors.
     * Format: {"statuscode": 400, "errormessage": {"field1": "message1", "field2": "message2"}}
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // Collect field-specific error messages
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        // Construct the standardized error response
        Map<String, Object> response = new HashMap<>();
        response.put("statuscode", HttpStatus.BAD_REQUEST.value());
        // Setting the detailed error map as the "errormessage" value
        response.put("errormessage", fieldErrors);
        return response;
    }

    /**
     * Handles cases where the request body is missing or malformed (e.g., non-JSON data,
     * missing fields in JSON that can't be defaulted), which causes HTTP message conversion failure.
     * Returns the standardized error response.
     * Format: {"statuscode": 400, "errormessage": "Missing or malformed request body."}
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("statuscode", HttpStatus.BAD_REQUEST.value());
        // Using a more explicit message for non-validation errors (like malformed JSON)
        response.put("errormessage", "Missing or malformed request body.");
        return response;
    }

    /**
     * Handles AccessDeniedException (thrown for unauthorized access, mapped to HTTP 403 Forbidden).
     * This exception is manually thrown in UserController's authorization checks.
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public Map<String, Object> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("statuscode", HttpStatus.FORBIDDEN.value());
        response.put("errormessage", ex.getMessage());
        return response;
    }

    /**
     * Handles all user-related Not Found exceptions (thrown when user ID or username/principal
     * is not found in the DB, mapped to HTTP 404 Not Found).
     * Uses a consistent "User not found." error message for both scenarios.
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({UsernameNotFoundException.class, UserNotFoundException.class})
    public Map<String, Object> handleUserNotFound(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("statuscode", HttpStatus.NOT_FOUND.value());
        // Consistent message for all 404 user lookups
        response.put("errormessage", "User not found.");
        return response;
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler({IllegalStateException.class})
    public Map<String, Object> handleConflict(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("statuscode", HttpStatus.CONFLICT.value());
        response.put("errormessage", ex.getMessage());
        return response;
    }
    /**
     * Handles custom status exceptions (e.g., 403 and 404) thrown via ResponseStatusException
     * in the AccountController.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatusException(ResponseStatusException ex, WebRequest request) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();

        ErrorResponse errorResponse = new ErrorResponse(
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorResponse, status);
    }


}

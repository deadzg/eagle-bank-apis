package com.eaglebank.api.exception;

/**
 * Custom runtime exception for when a requested User resource is not found by ID.
 * This exception will be caught by the ExceptionControllerAdvice to generate a consistent 404 response.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

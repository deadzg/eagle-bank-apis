package com.eaglebank.api.exception;

public class ResourceNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Resource Not Found";

    public ResourceNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    public ResourceNotFoundException(String exception) {
        super(exception);
    }
}

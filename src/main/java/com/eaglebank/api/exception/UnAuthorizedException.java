package com.eaglebank.api.exception;

import javax.naming.AuthenticationException;

public class UnAuthorizedException extends AuthenticationException {

    private static final String DEFAULT_MESSAGE = "401 Unauthorized ";

    public UnAuthorizedException() {
        super(DEFAULT_MESSAGE);
    }

    public UnAuthorizedException(String exception) {
        super(exception);
    }
}
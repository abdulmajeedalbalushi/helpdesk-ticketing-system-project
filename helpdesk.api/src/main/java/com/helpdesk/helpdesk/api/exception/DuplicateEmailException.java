package com.helpdesk.helpdesk.api.exception;

public class DuplicateEmailException  extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}

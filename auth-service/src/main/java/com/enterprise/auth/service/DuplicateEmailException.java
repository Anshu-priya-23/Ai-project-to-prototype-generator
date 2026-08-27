package com.enterprise.auth.service;

public class DuplicateEmailException extends RuntimeException {

    public static final String MESSAGE =
            "An account with this email already exists. Please sign in instead.";

    public DuplicateEmailException() {
        super(MESSAGE);
    }
}

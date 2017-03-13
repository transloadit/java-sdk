package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while generating request signature.
 */
public class LocalOperationException extends Exception {
    public LocalOperationException(Exception e) {
        super(e);
    }

    public LocalOperationException(String msg) {
        super(msg);
    }
}

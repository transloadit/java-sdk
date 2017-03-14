package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while doing a local Transloadit related operation.
 */
public class LocalOperationException extends Exception {
    public LocalOperationException(Exception e) {
        super(e);
    }

    public LocalOperationException(String msg) {
        super(msg);
    }
}

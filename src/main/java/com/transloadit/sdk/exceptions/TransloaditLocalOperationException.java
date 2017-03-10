package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while generating request signature.
 */
public class TransloaditLocalOperationException extends Exception {
    public TransloaditLocalOperationException(Exception e) {
        super(e);
    }

    public TransloaditLocalOperationException(String msg) {
        super(msg);
    }
}

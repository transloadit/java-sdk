package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while doing a local Transloadit related operation.
 */
public class LocalOperationException extends Exception {
    /**
     * Wraps an exception in a LocalOperationException.
     * @param e {@link Exception}
     */
    public LocalOperationException(Exception e) {
        super(e);
    }

    /**
     * Constructs a new LocalOperationException with the specified detail message.
     * @param msg Detail message
     */
    public LocalOperationException(String msg) {
        super(msg);
    }
}

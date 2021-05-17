package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while communicating with Transloadit API.
 */
public class RequestException extends Exception {
    /**
     * Wraps an exception in a RequestException.
     * @param e {@link Exception}
     */
    public RequestException(Exception e) {
        super(e);
    }

    /**
     * Constructs a new RequestException with the specified detail message.
     * @param msg Detail message
     */
    public RequestException(String msg) {
        super(msg);
    }
}

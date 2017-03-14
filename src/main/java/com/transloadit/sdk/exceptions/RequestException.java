package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while communicating with Transloadit API.
 */
public class RequestException extends Exception {
    public RequestException(Exception e) {
        super(e);
    }

    public RequestException(String msg) {
        super(msg);
    }
}

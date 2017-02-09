package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while communication with Transloadit API.
 */
public class TransloaditRequestException extends Exception {
    public TransloaditRequestException(Exception e) {
        super(e);
    }

    public TransloaditRequestException(String msg) {
        super(msg);
    }
}

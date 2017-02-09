package com.transloadit.sdk.exceptions;

/**
 * Exception thrown whenever something goes wrong while generating request signature.
 */
public class TransloaditSignatureException extends Exception {
    public TransloaditSignatureException(Exception e) {
        super(e);
    }

    public TransloaditSignatureException(String msg) {
        super(msg);
    }
}

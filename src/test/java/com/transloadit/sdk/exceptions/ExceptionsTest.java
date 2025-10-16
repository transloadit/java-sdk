package com.transloadit.sdk.exceptions;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Basic coverage tests for exception constructors.
 */
public class ExceptionsTest {
    @Test
    void requestExceptionConstructors() {
        Exception cause = new IllegalArgumentException("boom");
        RequestException wrapped = new RequestException(cause);
        Assertions.assertEquals(cause, wrapped.getCause());

        RequestException messageOnly = new RequestException("message");
        Assertions.assertEquals("message", messageOnly.getMessage());
    }

    @Test
    void localOperationExceptionConstructors() {
        Exception cause = new IllegalStateException("nope");
        LocalOperationException wrapped = new LocalOperationException(cause);
        Assertions.assertEquals(cause, wrapped.getCause());

        LocalOperationException messageOnly = new LocalOperationException("message");
        Assertions.assertEquals("message", messageOnly.getMessage());

        LocalOperationException messageAndCause = new LocalOperationException("detail", cause);
        Assertions.assertEquals("detail", messageAndCause.getMessage());
        Assertions.assertEquals(cause, messageAndCause.getCause());
    }
}

package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link SignatureProvider} integration with {@link Transloadit} and {@link Request}.
 */
public class SignatureProviderTest {
    private static final String TEST_SIGNATURE = "sha384:external-signature";

    @Test
    void signatureProviderConstructorsEnableSigning() {
        SignatureProvider provider = params -> TEST_SIGNATURE;

        Transloadit withHost = new Transloadit("KEY", provider, 60, "http://example.com");
        Assertions.assertSame(provider, withHost.getSignatureProvider());
        Assertions.assertTrue(withHost.shouldSignRequest);
        Assertions.assertNull(withHost.secret);
        Assertions.assertEquals("http://example.com", withHost.getHostUrl());

        Transloadit withDefaults = new Transloadit("KEY", provider);
        Assertions.assertSame(provider, withDefaults.getSignatureProvider());
        Assertions.assertTrue(withDefaults.shouldSignRequest);
        Assertions.assertNull(withDefaults.secret);
        Assertions.assertEquals(5 * 60, withDefaults.duration);
        Assertions.assertEquals(Transloadit.DEFAULT_HOST_URL, withDefaults.getHostUrl());
    }

    @Test
    void setSignatureProviderTogglesSigningBasedOnAvailability() throws LocalOperationException {
        SignatureProvider provider = params -> TEST_SIGNATURE;
        Transloadit transloadit = new Transloadit("KEY", "SECRET");

        transloadit.setSignatureProvider(provider);
        Assertions.assertSame(provider, transloadit.getSignatureProvider());
        Assertions.assertTrue(transloadit.shouldSignRequest);

        transloadit.setSignatureProvider(null);
        Assertions.assertNull(transloadit.getSignatureProvider());
        Assertions.assertTrue(transloadit.shouldSignRequest); // falls back to secret-based signing

        Transloadit withoutSecret = new Transloadit("KEY", provider);
        Assertions.assertTrue(withoutSecret.shouldSignRequest);
        withoutSecret.setSignatureProvider(null);
        Assertions.assertFalse(withoutSecret.shouldSignRequest);
    }

    @Test
    void toPayloadUsesSignatureFromProvider() throws Exception {
        AtomicReference<String> capturedParams = new AtomicReference<>();
        SignatureProvider provider = paramsJson -> {
            capturedParams.set(paramsJson);
            return TEST_SIGNATURE;
        };

        Transloadit transloadit = new Transloadit("KEY", provider);
        Request request = new Request(transloadit);

        Map<String, Object> data = new HashMap<>();
        data.put("template_id", "123");
        data.put("expires", Instant.now().toString());

        Map<String, String> payload = invokeToPayload(request, data);
        Assertions.assertEquals(TEST_SIGNATURE, payload.get("signature"));

        String paramsJson = payload.get("params");
        Assertions.assertNotNull(paramsJson);
        Assertions.assertEquals(paramsJson, capturedParams.get());

        JSONObject params = new JSONObject(paramsJson);
        Assertions.assertEquals("123", params.get("template_id"));
        Assertions.assertTrue(params.has("auth"));
        Assertions.assertTrue(params.has("nonce"));
    }

    @Test
    void toPayloadWrapsProviderExceptions() throws Exception {
        SignatureProvider provider = params -> {
            throw new IllegalStateException("backend unavailable");
        };
        Transloadit transloadit = new Transloadit("KEY", provider);
        Request request = new Request(transloadit);

        Map<String, Object> data = new HashMap<>();
        InvocationTargetException invocationTargetException = Assertions.assertThrows(InvocationTargetException.class,
                () -> invokeToPayload(request, data));

        Throwable cause = invocationTargetException.getCause();
        Assertions.assertTrue(cause instanceof LocalOperationException);
        Assertions.assertEquals("Failed to generate signature using provider.", cause.getMessage());
        Assertions.assertEquals(IllegalStateException.class, cause.getCause().getClass());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> invokeToPayload(@NotNull Request request, Map<String, Object> data) throws Exception {
        Method method = Request.class.getDeclaredMethod("toPayload", Map.class);
        method.setAccessible(true);
        return (Map<String, String>) method.invoke(request, data);
    }
}

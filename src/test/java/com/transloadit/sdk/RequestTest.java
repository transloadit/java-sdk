package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.json.JSONObject;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static org.mockserver.model.HttpError.error;

/**
 * Unit test for {@link Request} class. Api-Responses are simulated by mocking the server's response.
 */
@ExtendWith(MockServerExtension.class)  // MockServerExtension is used to start and stop the MockServer
@MockServerSettings(ports = MockHttpService.PORT) // MockServerSettings is used to define the port of the MockServer
public class RequestTest extends MockHttpService {
    /**
     * Links to {@link Request} instance to perform the tests on.
     */
    private Request request;
    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private final MockServerClient mockServerClient = new MockServerClient("localhost", MockHttpService.PORT);

    /**
     * Assings a new {@link Request} instance to Request variable before each individual test and resets
     * the mockServerClient.
     */
    @BeforeEach
    public void setUp() throws Exception {
        request = new Request(transloadit);
        mockServerClient.reset();
    }

    private JSONObject runSmartSig(String paramsJson, String key, String secret) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("npx", "--yes", "transloadit@4.0.4", "smart_sig");
        builder.environment().put("TRANSLOADIT_KEY", key);
        builder.environment().put("TRANSLOADIT_SECRET", secret);

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            Assumptions.assumeTrue(false, "npx not available: " + e.getMessage());
            return new JSONObject();
        }

        try (OutputStream os = process.getOutputStream()) {
            os.write(paramsJson.getBytes(StandardCharsets.UTF_8));
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        int status = process.waitFor();
        if (status != 0) {
            Assertions.fail("smart_sig CLI failed: " + stderr);
        }
        return new JSONObject(stdout);
    }

    private String extractMultipartField(String body, String fieldName) {
        String token = "name=\"" + fieldName + "\"";
        int nameIndex = body.indexOf(token);
        if (nameIndex == -1) {
            return null;
        }

        int headerEnd = body.indexOf("\r\n\r\n", nameIndex);
        int delimiterLength = 4;
        if (headerEnd == -1) {
            headerEnd = body.indexOf("\n\n", nameIndex);
            delimiterLength = 2;
        }
        if (headerEnd == -1) {
            return null;
        }

        int valueStart = headerEnd + delimiterLength;
        int boundaryIndex = body.indexOf("\r\n--", valueStart);
        if (boundaryIndex == -1) {
            boundaryIndex = body.indexOf("\n--", valueStart);
        }
        if (boundaryIndex == -1) {
            boundaryIndex = body.length();
        }

        return body.substring(valueStart, boundaryIndex).trim();
    }

    /**
     * Checks the result of the {@link Request#get(String)}  method by verifying the format of the GET request
     * the MockServer receives.
     * @throws Exception if request building goes wrong.
     */
    @Test
    public void get() throws Exception {
        request.get("/foo");

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo")
                .withMethod("GET")
                .withHeader("Transloadit-Client", "java-sdk:2.1.0"));

    }

    /**
     * Checks the result of the {@link Request#post(String, Map)}  method by verifying the format of the
     * POST request the MockServer receives.
     * @throws Exception if request building goes wrong.
     */
    @Test
    public void post() throws Exception {
        request.post("/foo", new HashMap<String, Object>());

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo").withMethod("POST"));
    }

    /**
     * Checks the result of the {@link Request#delete(String, Map)} )}  method by verifying the format of the
     * DELETE request the MockServer receives.
     * @throws Exception if request building goes wrong.
     */
    @Test
    public void delete() throws Exception {
        request.delete("/foo", new HashMap<String, Object>());

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo").withMethod("DELETE"));
    }

    /**
     * Checks the result of the {@link Request#put(String, Map)}  method by verifying the format of the PUT request
     * the MockServer receives.
     * @throws Exception if request building goes wrong.on
     */
    @Test
    public void put() throws Exception {
        request.put("/foo", new HashMap<String, Object>());

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo").withMethod("PUT"));
    }

    /**
     * Tests if the method {@link Request#qualifiedForRetry(Exception)} determines correctly if a retry attempt after an
     * exception should be performed.
     * Special test environment needed as it could interfere with other tests.
     */
    @Test
    public void qualifiedForRetry() {
        Transloadit transloadit2 = new Transloadit("KEY", "SECRET",
                "http://localhost:" + 9040);
        transloadit2.setRetryAttemptsRequestException(5); // Test if it works with defined errors
        Request newRequest = new Request(transloadit2);
        Exception e = new SocketTimeoutException("connect timed out");
        Assertions.assertTrue(newRequest.qualifiedForRetry(e));
        Assertions.assertEquals(4, newRequest.retryAttemptsRequestExceptionLeft); // tests counting logic

        Exception e2 = new ArrayStoreException("foo bar"); // shouldn't work here
        Assertions.assertFalse(newRequest.qualifiedForRetry(e2));

        transloadit2.setRetryAttemptsRequestException(0);
        newRequest = new Request(transloadit2);
        Assertions.assertFalse(newRequest.qualifiedForRetry(e));
    }

    /**
     * Tests if {@link Request} retries correctly after an exception.
     * Also need special settings for each test.
     * With one retry set you will have 3 attempts per request (1x Initial, 1 retry by OkHttp, 1x Retry by function)
     */
    @Test
    public void retryAfterSpecificErrors() throws LocalOperationException, RequestException {
        Transloadit transloadit2 = new Transloadit("KEY", "SECRET",
                "http://localhost:" + 9040);

        ArrayList<String> errors = transloadit2.getQualifiedErrorsForRetry();
        errors.add("java.io.IOException: unexpected end of stream on http://localhost:9040/");
        transloadit2.setQualifiedErrorsForRetry(errors);
        transloadit2.setRetryAttemptsRequestException(1);

        // GET REQUESTS
        Request testRequest = new Request(transloadit2);
        mockServerClient.when(HttpRequest.request()
              .withPath("/foo").withMethod("GET"), Times.exactly(3)).error(
               error().withDropConnection(true));
        testRequest.get("/foo");

        //mockServerClient.verify(HttpRequest.request("/foo").withMethod("GET"));

        // POST REQUESTS
        testRequest = new Request(transloadit2);
        mockServerClient.when(HttpRequest.request()
                .withPath("/foo").withMethod("POST"), Times.exactly(3)).error(
                error().withDropConnection(true));
        testRequest.post("/foo", new HashMap<String, Object>());

        // PUT REQUEST
        testRequest = new Request(transloadit2);
        mockServerClient.when(HttpRequest.request()
                .withPath("/foo").withMethod("PUT"), Times.exactly(3)).error(
                error().withDropConnection(true));
        testRequest.put("/foo", new HashMap<String, Object>());

        // DELETE REQUEST
        testRequest = new Request(transloadit2);
        mockServerClient.when(HttpRequest.request()
                .withPath("/foo").withMethod("DELETE"), Times.exactly(3)).error(
                error().withDropConnection(true));
        testRequest.delete("/foo", new HashMap<String, Object>());
    }

    /**
     * Verifies that Request routes params through the custom SignatureProvider.
     */
    @Test
    public void postUsesSignatureProviderWhenPresent() throws Exception {
        final boolean[] invoked = {false};
        final String expectedSignature = "providedSignature";
        SignatureProvider provider = params -> {
            invoked[0] = true;
            return expectedSignature;
        };

        Transloadit client = new Transloadit("KEY", provider, "http://localhost:" + PORT);
        Request providerRequest = new Request(client);

        mockServerClient.when(HttpRequest.request().withPath("/signature-test").withMethod("POST"))
                .respond(HttpResponse.response().withStatusCode(200));

        providerRequest.post("/signature-test", new HashMap<>());

        HttpRequest[] recorded = mockServerClient.retrieveRecordedRequests(HttpRequest.request()
                .withPath("/signature-test").withMethod("POST"));
        String body = recorded[0].getBodyAsString();

        Assertions.assertTrue(invoked[0], "Signature provider should be called");
        Assertions.assertTrue(body.contains(expectedSignature), "Signature should come from provider");
    }

    /**
     * Built-in signing should match the Node smart_sig CLI output.
     */
    @Test
    public void payloadSignatureMatchesSmartSigCli() throws Exception {
        String key = "cli_key";
        String secret = "cli_secret";
        Transloadit client = new Transloadit(key, secret, "http://localhost:" + PORT);
        Request localRequest = new Request(client);

        HashMap<String, Object> params = new HashMap<String, Object>();

        mockServerClient.when(HttpRequest.request().withPath("/cli-sign").withMethod("POST"))
                .respond(HttpResponse.response().withStatusCode(200));

        localRequest.post("/cli-sign", params);

        HttpRequest[] recorded = mockServerClient.retrieveRecordedRequests(HttpRequest.request()
                .withPath("/cli-sign").withMethod("POST"));
        String body = recorded[0].getBodyAsString();
        String paramsJson = extractMultipartField(body, "params");
        String signature = extractMultipartField(body, "signature");

        Assertions.assertNotNull(paramsJson, "params payload missing: " + body);
        Assertions.assertNotNull(signature, "signature missing: " + body);

        JSONObject cliResult = runSmartSig(paramsJson, key, secret);
        Assertions.assertEquals(paramsJson, cliResult.getString("params"), "CLI params mismatch: " + cliResult);
        Assertions.assertEquals(signature, cliResult.getString("signature"), "CLI signature mismatch: " + cliResult + " javaParams=" + paramsJson);
    }

    /**
     * When signing is disabled, no signature parameter should be added.
     */
    @Test
    public void toPayloadOmitsSignatureWhenSigningDisabled() throws Exception {
        Transloadit client = new Transloadit("KEY", "SECRET", "http://localhost:" + PORT);
        client.setRequestSigning(false);
        Request localRequest = new Request(client);

        mockServerClient.when(HttpRequest.request().withPath("/no-sign").withMethod("POST"))
                .respond(HttpResponse.response().withStatusCode(200));

        localRequest.post("/no-sign", new HashMap<>());

        HttpRequest[] recorded = mockServerClient.retrieveRecordedRequests(HttpRequest.request()
                .withPath("/no-sign").withMethod("POST"));
        Assertions.assertFalse(recorded[0].getBodyAsString().contains("signature"));
    }

    /**
     * Ensures provider exceptions are surfaced as LocalOperationException.
     */
    @Test
    public void signatureProviderExceptionIsWrapped() {
        SignatureProvider provider = params -> {
            throw new Exception("boom");
        };
        Transloadit client = new Transloadit("KEY", provider, "http://localhost:" + PORT);
        Request providerRequest = new Request(client);

        Assertions.assertThrows(LocalOperationException.class, () ->
                providerRequest.post("/signature-error", new HashMap<>()));
    }

    /**
     * Test secure nonce generation with.
     */
    @Test
    public void getNonce() {
        String cipher = "Blowfish";
        int keyLength = 256;

        String nonce = request.getNonce(cipher, keyLength);
        Assertions.assertEquals(44, nonce.length());
    }

    /**
     * Tests if {@link Request#delayBeforeRetry()} works.
     * @throws LocalOperationException
     */
    @Test
    public void delayBeforeRetry() throws LocalOperationException {
        long startTime;
        long endTime;
        startTime = System.currentTimeMillis();
        int timeout = request.delayBeforeRetry();
        endTime = System.currentTimeMillis();
        int delta = (int) (endTime - startTime);
        Assertions.assertTrue(delta >= timeout);

    }
}

package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;

import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
//CHECKSTYLE:OFF
import java.util.Map;  // Suppress warning as the Map import is needed for the JavaDoc Comments

import static org.mockserver.model.HttpError.error;
//CHECKSTYLE:ON

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
                .withHeader("Transloadit-Client", "java-sdk:1.0.0"));

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

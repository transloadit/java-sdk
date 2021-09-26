package com.transloadit.sdk;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import java.util.HashMap;
//CHECKSTYLE:OFF
import java.util.Map;  // Suppress warning as the Map import is needed for the JavaDoc Comments
//CHECKSTYLE:ON

/**
 * Unit test for {@link Request} class. Api-Responses are simulated by mocking the server's response.
 */
public class RequestTest extends MockHttpService {
    /**
     * Links to {@link Request} instance to perform the tests on.
     */
    private Request request;

    /**
     * MockServer can be run using the MockServerRule.
     */
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, true, PORT);

    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private MockServerClient mockServerClient;

    /**
     * Assings a new {@link Request} instance to Request variable before each individual test and resets
     * the mockServerClient.
     */
    @Before
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
                .withHeader("Transloadit-Client", "java-sdk:0.4.0"));

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

}


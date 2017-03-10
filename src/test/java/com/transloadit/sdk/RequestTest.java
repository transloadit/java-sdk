package com.transloadit.sdk;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import java.util.HashMap;

/**
 * test Request class.
 */
public class RequestTest extends MockHttpService {
    public Request request;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    @Before
    public void setUp() throws Exception {
        request = new Request(transloadit);
    }


    @Test
    public void testGet() throws Exception {
        request.get("/foo");

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo").withMethod("GET"));

    }

    @Test
    public void testPost() throws Exception {
        request.post("/foo", new HashMap<String, Object>());

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo").withMethod("POST"));
    }


    @Test
    public void testDelete() throws Exception {
        request.delete("/foo", new HashMap<String, Object>());

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo").withMethod("DELETE"));
    }

    @Test
    public void testPut() throws Exception {
        request.put("/foo", new HashMap<String, Object>());

        mockServerClient.verify(HttpRequest.request()
                .withPath("/foo").withMethod("PUT"));
    }

}
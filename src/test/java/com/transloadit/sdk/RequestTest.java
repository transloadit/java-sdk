package com.transloadit.sdk;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * test Request class.
 */
public class RequestTest {
    public Request request;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9040);

    @Before
    public void setUp() throws Exception {
        Transloadit transloadit = new Transloadit("key", "secret", "http://localhost:9040");
        request = new Request(transloadit);

        // stub returns no response when this request is run too quickly.
        Thread.sleep(2000);
    }


    @Test
    public void testGet() throws Exception {
        stubFor(get(urlPathEqualTo("/foo"))
                .willReturn(aResponse().withBody("{}")));

        request.get("/foo");

        verify(getRequestedFor(urlPathEqualTo("/foo")));
    }

    @Test
    public void testPost() throws Exception {
        stubFor(post(urlPathEqualTo("/foo"))
                .willReturn(aResponse().withBody("{}")));

        request.post("/foo", new HashMap<String, Object>());

        verify(postRequestedFor(urlPathEqualTo("/foo")));
    }


    @Test
    public void testDelete() throws Exception {
        stubFor(delete(urlPathEqualTo("/foo"))
                .willReturn(aResponse().withBody("{}")));

        request.delete("/foo", new HashMap<String, Object>());

        verify(deleteRequestedFor(urlPathEqualTo("/foo")));
    }

    @Test
    public void testPut() throws Exception {
        stubFor(put(urlPathEqualTo("/foo"))
                .willReturn(aResponse().withBody("{}")));

        request.put("/foo", new HashMap<String, Object>());

        verify(putRequestedFor(urlPathEqualTo("/foo")));
    }

}
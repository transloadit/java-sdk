package com.transloadit.sdk.response;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.MockHttpService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.junit.Assert.assertEquals;

/**
 * Unit Test class for {@link Response}. Api-Responses are simulated by mocking the server's response.
 */
public class ResponseTest extends MockHttpService {
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
     * Resets MockserverClient before each Test run.
     */
    @Before
    public void setUp() {
        mockServerClient.reset();
    }

    /**
     * This Test checks if the {@link Response#json()} method converts a received server response body
     * to a valid {@link org.json.JSONObject}.
     * @throws Exception if the Test resource "assembly.json" is missing, a JSON Key cannot be found or if HTTP or
     * non-HTTP errors have occurred.
     */
    @Test
    public void json() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        AssemblyResponse response = new Assembly(transloadit).save(false);
        assertEquals(response.json().getString("ok"), "ASSEMBLY_COMPLETED");
    }

    /**
     * This Test checks if {@link Response#status()} returns status code from a defined server response.
     * @throws Exception  if the Test resource "assembly.json" is missing or if HTTP or non-HTTP errors have occurred.
     */
    @Test
    public void status() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        AssemblyResponse response = new Assembly(transloadit).save(false);
        assertEquals(response.status(), 200);
    }

}

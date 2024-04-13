package com.transloadit.sdk.response;

import com.transloadit.sdk.MockHttpService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.junit.Assert.assertEquals;

/**
 * Unit Test class for {@link Response}. Api-Responses are simulated by mocking the server's response.
 */
@ExtendWith(MockServerExtension.class)  // MockServerExtension is used to start and stop the MockServer
@MockServerSettings(ports = MockHttpService.PORT) // MockServerSettings is used to define the port of the MockServer
public class ResponseTest extends MockHttpService {
    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private final MockServerClient mockServerClient = new MockServerClient("localhost", PORT);

    /**
     * Resets MockserverClient before each Test run.
     */
    @BeforeEach
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

        AssemblyResponse response = newAssemblyWithoutID().save(false);
        Assertions.assertEquals(response.json().getString("ok"), "ASSEMBLY_COMPLETED");
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

        AssemblyResponse response = newAssemblyWithoutID().save(false);
        Assertions.assertEquals(response.status(), 200);
    }

}

package com.transloadit.sdk.response;

import com.transloadit.sdk.MockHttpService;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;


/**
 * Unit Test class for {@link AssemblyResponse}. Api-Responses are simulated by mocking the server's response.
 */
@ExtendWith(MockServerExtension.class)  // MockServerExtension is used to start and stop the MockServer
@MockServerSettings(ports = MockHttpService.PORT) // MockServerSettings is used to define the port of the MockServer
public class AssemblyResponseTest extends MockHttpService {
    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private final MockServerClient mockServerClient = new MockServerClient("localhost", MockHttpService.PORT);

    /**
     * Links to {@link AssemblyResponse} instance to perform the tests on.
     */
    private AssemblyResponse response;

    /**
     * Instantiates a new {@link AssemblyResponse} instance by mocking an Assembly execution and resets the MockServer
     * before each test.
     * @throws Exception if Test resource "assembly.json" is missing.
     */
    @BeforeEach
    public void setUp() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));
        response = newAssemblyWithoutID().save();
        mockServerClient.reset();
    }

    /**
     * Resets the mockServerClient after each run.
     */
    @AfterEach
    public void tearDown() {
        mockServerClient.reset();
    }

    /**
     * Checks if {@link AssemblyResponse#getId()} returns the expected Assembly - ID.
     */
    @Test
    public void getId() {
        Assertions.assertEquals(response.getId(), "76fe5df1c93a0a530f3e583805cf98b4");
    }

    /**
     * Checks if {@link AssemblyResponse#getId()} returns the expected Assembly - URL from the server response's
     * assembly_url field.
     */
    @Test
    public void getUrl() {
        Assertions.assertEquals(response.getUrl(), "http://localhost:9040/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    /**
     * Checks if {@link AssemblyResponse#getId()} returns the expected Assembly - URL from the server response's
     * assembly_url field.
     */
    @Test
    public void getSslUrl() {
        Assertions.assertEquals(response.getSslUrl(), "http://localhost:9040/assemblies/76fe5df1c93a0a530f3e583805cf98b4");
    }

    /**
     * Checks if the {@link AssemblyResponse#getStepResult(String)} returns the correct URL.
     */
    @Test
    public void getStepResult() {
        Assertions.assertEquals(response.getStepResult("resize").getJSONObject(0).getString("url"),
                "http://tmp.jane.transloadit.com/result_url.png");
    }

    /**
     * Checks if {@link AssemblyResponse#isCompleted()} returns the correct value.
     */
    @Test
    public void isCompleted() {
        Assertions.assertTrue(response.isCompleted());
    }

    /**
     * Checks if {@link AssemblyResponse#isAborted()} returns {@code true} after receiving a server response telling,
     * that the Assembly was aborted.
     * @throws Exception if Local or HTTP actions are going wrong.
     */
    @Test
    public void isAborted() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"REQUEST_ABORTED\",\n"
                                + "\"assembly_id\":\"id_value\",\n"
                                + "\"assembly_url\":\"foo\",\n"
                                + "\"assembly_ssl_url\":\"bar\"}"));

        AssemblyResponse abortedResponse = newAssemblyWithoutID().save(false);

        Assertions.assertTrue(abortedResponse.isAborted());
        Assertions.assertFalse(response.isAborted());
    }

    /**
     * Identical to {@link AssemblyResponseTest#isAborted()} except, that the server response sends an
     * "ASSEMBLY_CANCELED" status.
     * @throws Exception Exception if Local or HTTP actions are going wrong.
     */
    @Test
    public void isCanceled() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"ASSEMBLY_CANCELED\",\n"
                                + "\"assembly_id\":\"id_value\",\n"
                                + "\"assembly_url\":\"foo\",\n"
                                + "\"assembly_ssl_url\":\"bar\"}"));

        AssemblyResponse cancelledResponse = newAssemblyWithoutID().save(false);

        Assertions.assertTrue(cancelledResponse.isCanceled());
        Assertions.assertFalse(response.isCanceled());
    }

    /**
     * Identical to {@link AssemblyResponseTest#isAborted()} except, that the server response sends an
     * "ASSEMBLY_EXECUTING" status.
     * @throws Exception Exception if Local or HTTP actions are going wrong.
     */
    @Test
    public void isExecuting() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"ASSEMBLY_EXECUTING\",\n"
                                + "\"assembly_id\":\"id_value\",\n"
                                + "\"assembly_url\":\"foo\",\n"
                                + "\"assembly_ssl_url\":\"bar\"}"));

        AssemblyResponse executingResponse = newAssemblyWithoutID().save(false);

        Assertions.assertTrue(executingResponse.isExecuting());
        Assertions.assertFalse(response.isExecuting());
    }

    /**
     * This test checks if {@link AssemblyResponse#isFinished()} returns {@code false} if an Assembly is still executing
     * and that the method returns {@code true} if the server response contains the "ASSEMBLY_COMPLETED" status.
     */
    @Test
    public void isFinished() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"ASSEMBLY_EXECUTING\",\n"
                                + "\"assembly_id\":\"id_value\",\n"
                                + "\"assembly_url\":\"foo\",\n"
                                + "\"assembly_ssl_url\":\"bar\"}"));


        AssemblyResponse executingResponse = newAssemblyWithoutID().save(false);

        Assertions.assertFalse(executingResponse.isFinished());
        Assertions.assertTrue(response.isFinished());
    }
}

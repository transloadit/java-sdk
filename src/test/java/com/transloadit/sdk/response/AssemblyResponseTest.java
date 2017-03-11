package com.transloadit.sdk.response;

import com.transloadit.sdk.Assembly;
import com.transloadit.sdk.MockHttpService;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AssemblyResponseTest extends MockHttpService {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    public AssemblyResponse response;


    @Before
    public void setUp() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        response = new Assembly(transloadit).save(false);
        mockServerClient.reset();
    }

    @After
    public void tearDown() {
        mockServerClient.reset();
    }

    @Test
    public void reload() throws Exception {
        response.reload();

        mockServerClient.verify(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"));
    }

    @Test
    public void cancel() throws Exception {
        response.cancel();

        mockServerClient.verify(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("DELETE"));
    }

    @Test
    public void isCompleted() throws Exception {
        assertTrue(response.isCompleted());
    }

    @Test
    public void isAborted() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"REQUEST_ABORTED\",\n" +
                                "\"assembly_id\":\"id_value\",\n" +
                                "\"assembly_url\":\"foo\",\n" +
                                "\"assembly_ssl_url\":\"bar\"}"));

        AssemblyResponse abortedResponse = new Assembly(transloadit).save(false);

        assertTrue(abortedResponse.isAborted());
        assertFalse(response.isAborted());
    }

    @Test
    public void isCanceled() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"ASSEMBLY_CANCELED\",\n" +
                                "\"assembly_id\":\"id_value\",\n" +
                                "\"assembly_url\":\"foo\",\n" +
                                "\"assembly_ssl_url\":\"bar\"}"));

        AssemblyResponse cancelledResponse = new Assembly(transloadit).save(false);

        assertTrue(cancelledResponse.isCanceled());
        assertFalse(response.isCanceled());
    }

    @Test
    public void isExecuting() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"ASSEMBLY_EXECUTING\",\n" +
                                "\"assembly_id\":\"id_value\",\n" +
                                "\"assembly_url\":\"foo\",\n" +
                                "\"assembly_ssl_url\":\"bar\"}"));

        AssemblyResponse executingResponse = new Assembly(transloadit).save(false);

        assertTrue(executingResponse.isExecuting());
        assertFalse(response.isExecuting());
    }

    @Test
    public void isFinished() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withBody("{\"ok\":\"ASSEMBLY_EXECUTING\",\n" +
                                "\"assembly_id\":\"id_value\",\n" +
                                "\"assembly_url\":\"foo\",\n" +
                                "\"assembly_ssl_url\":\"bar\"}"));

        AssemblyResponse executingResponse = new Assembly(transloadit).save(false);

        assertFalse(executingResponse.isFinished());
        assertTrue(response.isFinished());
    }

}
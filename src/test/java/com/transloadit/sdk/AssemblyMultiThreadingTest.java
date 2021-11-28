package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for the multi threading features of {@link Assembly Assembly.class}.
 */
public class AssemblyMultiThreadingTest extends MockHttpService {
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
     * Links to {@link Assembly} instance to perform the tests on.
     */
    private Assembly assembly;
    /**
     * Indicates if the {@link Assembly} has been finished.
     */
    private boolean assemblyFinished;

    /**
     * Assigns a new {@link Assembly} instance to the {@link AssemblyMultiThreadingTest#assembly} variable before each
     * individual test
     * and resets the mockServerClient. Also sets {@link AssemblyMultiThreadingTest#assemblyFinished} {@code = false}.
     */
    @Before
    public void setUp() throws Exception {
        assembly = newAssemblyWithoutID();
        assemblyFinished = false;
        mockServerClient.reset();
    }
    /**
     * Saves a multithreaded upload and verifies that the requests are sent.
     * @throws IOException
     * @throws LocalOperationException
     * @throws RequestException
     */
    @Test
    public void saveMultiThreadedUpload() throws IOException, LocalOperationException, RequestException {
        MockTusAssemblyMultiThreading assembly = new MockTusAssemblyMultiThreading(transloadit);
        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.addFile(new File("LICENSE"), "file_name2");
        assembly.setMaxParallelUploads(2);

        String uploadSize = "1077";
        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        mockServerClient.when(
                HttpRequest.request()
                        .withPath("/resumable/files").withMethod("POST"), Times.exactly(1)).respond(
                new HttpResponse()
                        .withStatusCode(201)
                        .withHeader("Tus-Resumable", "1.0.0").withHeader(
                                "Location", "http://localhost:9040/resumable/files/2"));
        mockServerClient.when(
                HttpRequest.request()
                        .withPath("/resumable/files").withMethod("POST"), Times.exactly(1)).respond(
                new HttpResponse()
                        .withStatusCode(201)
                        .withHeader("Tus-Resumable", "1.0.0").withHeader(
                                "Location", "http://localhost:9040/resumable/files/1"));


        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files/1").withMethod("PATCH").withHeader(
                        "Upload-Length")).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", uploadSize));

        AssemblyResponse response = assembly.save(true);

        assertEquals(response.json().get("assembly_id"), "02ce6150ea2811e6a35a8d1e061a5b71");
        assertEquals(response.json().get("ok"), "ASSEMBLY_UPLOADING");
    }
}

package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.mockserver.model.RegexBody.regex;

/**
 * Unit test for the multi threading features of {@link Assembly Assembly.class}.
 */
@ExtendWith(MockServerExtension.class)  // MockServerExtension is used to start and stop the MockServer
@MockServerSettings(ports = MockHttpService.PORT) // MockServerSettings is used to define the port of the MockServer
public class AssemblyMultiThreadingTest extends MockHttpService {
    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private final MockServerClient mockServerClient = new MockServerClient("localhost", PORT);

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
    @BeforeEach
    public void setUp() throws Exception {
        assembly = new MockTusAssemblyMultiThreading(transloadit);
        assembly.wipeAssemblyID();
        assemblyFinished = false;
        mockServerClient.reset();
        assembly.addFile(new File(getClass().getResource("/__files/assembly_executing.json").getFile()));
        assembly.addFile(Files.newInputStream(Paths.get(
                getClass().getResource("/__files/cancel_assembly.json").getFile())));
    }
    /**
     * Saves a multithreaded upload and verifies that the requests are sent.
     * @throws IOException - Thrown if an I/O error occurs while loading test resources.
     * @throws LocalOperationException - If a local non-http operation fails.
     * @throws RequestException -  If a request to Transloadit server fails.
     */
    @Test
    public void saveMultiThreadedUpload() throws IOException, LocalOperationException, RequestException {
        MockTusAssemblyMultiThreading assembly = new MockTusAssemblyMultiThreading(transloadit);
        assembly.wipeAssemblyID();
        assembly.setRunnableAssemblyListener(new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {

            }

            @Override
            public void onError(Exception error) {

            }

            @Override
            public void onMetadataExtracted() {

            }

            @Override
            public void onAssemblyUploadFinished() {

            }

            @Override
            public void onFileUploadFinished(JSONObject uploadInformation) {

            }

            @Override
            public void onFileUploadPaused(String name) {

            }

            @Override
            public void onFileUploadResumed(String name) {

            }

            @Override
            public void onFileUploadProgress(long uploadedBytes, long totalBytes) {

            }

            @Override
            public void onAssemblyProgress(JSONObject progress) {

            }

            @Override
            public void onAssemblyResultFinished(JSONArray result) {

            }
        });

        assembly.addFile(new File("LICENSE"), "file_name1");
        assembly.addFile(new File("LICENSE"), "file_name2");
        assembly.addFile(new File("LICENSE"), "file_name3");
        assembly.setMaxParallelUploads(3);
        String uploadSize = "" + new File("LICENSE").length();
        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies")
                        .withMethod("POST"))
                        .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));


        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0"), Times.once()).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Location", "/resumable/files/a"));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0"), Times.once()).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Location", "/resumable/files/b"));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0"), Times.once()).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Location", "/resumable/files/c"));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files/a").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0")
                .withHeader("Upload-Offset", "0")
                .withHeader("X-HTTP-Method-Override", "PATCH")).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", uploadSize));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files/b").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0")
                .withHeader("Upload-Offset", "0")
                .withHeader("X-HTTP-Method-Override", "PATCH")).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", uploadSize));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files/c").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0")
                .withHeader("Upload-Offset", "0")
                .withHeader("X-HTTP-Method-Override", "PATCH")).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", uploadSize));

        AssemblyResponse response = assembly.save(true);
        mockServerClient.verify(HttpRequest.request()
                .withPath("/resumable/files").withMethod("POST"), VerificationTimes.atLeast(3));

        Assertions.assertEquals("02ce6150ea2811e6a35a8d1e061a5b71", response.json().get("assembly_id"));
        Assertions.assertEquals("ASSEMBLY_UPLOADING", response.json().get("ok"));
    }

    /**
     * Checks if {@link Assembly#getRunnableAssemblyListener()} and
     * {@link Assembly#setRunnableAssemblyListener(AssemblyListener)} are working.
     */
    @Test
    public void getAndSetUploadProgressListener() {
        AssemblyListener listener = new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {

            }

            @Override
            public void onError(Exception error) {

            }

            @Override
            public void onMetadataExtracted() {

            }

            @Override
            public void onAssemblyUploadFinished() {

            }

            @Override
            public void onFileUploadFinished(JSONObject uploadInformation) {

            }

            @Override
            public void onFileUploadPaused(String name) {

            }

            @Override
            public void onFileUploadResumed(String name) {

            }

            @Override
            public void onFileUploadProgress(long uploadedBytes, long totalBytes) {

            }

            @Override
            public void onAssemblyProgress(JSONObject progress) {

            }

            @Override
            public void onAssemblyResultFinished(JSONArray result) {

            }

        };
        assembly.setRunnableAssemblyListener(listener);
        Assertions.assertEquals(listener, assembly.getRunnableAssemblyListener());
    }

    /**
     * Verifies, that {@link Assembly#abortUploads()} gets called in case of an upload error.
     * And if the {@link AssemblyListener} gets notified.
     * @throws IOException - Thrown if an I/O error occurs while loading test resources.
     * @throws LocalOperationException - If a local non-http operation fails.
     * @throws RequestException -  If a request to Transloadit server fails.
     */
    @Test
    public void abortUploads() throws LocalOperationException, RequestException, IOException, InterruptedException {
        AssemblyListener listener = getEmptyAssemblyListener();
        AssemblyListener spyListener = Mockito.spy(listener);

        MockProtocolExceptionAssembly exceptionAssembly = new MockProtocolExceptionAssembly(transloadit);
        exceptionAssembly.wipeAssemblyID();
        exceptionAssembly.setMaxParallelUploads(2);
        exceptionAssembly.addFile(new File(getClass().getResource("/__files/assembly_executing.json").getFile()));

        Assembly assemblySpy = Mockito.spy(exceptionAssembly);
        assemblySpy.setRunnableAssemblyListener(spyListener);

        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies")
                        .withMethod("POST")
                        .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                                + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        ArgumentCaptor<LocalOperationException> exceptionArgumentCaptor =
                ArgumentCaptor.forClass(LocalOperationException.class);

        assemblySpy.save(true);

        Thread.sleep(500);  // This avoids a too fast test completion, before the abortion could take place
        Mockito.verify(assemblySpy).abortUploads();
        Mockito.verify(spyListener).onError(exceptionArgumentCaptor.capture());
        String errorMessage = "Uploads aborted";
        String exceptionMessage = exceptionArgumentCaptor.getValue().getMessage();
        Assertions.assertEquals(errorMessage, exceptionMessage);
    }

    /**
     * Checks, if uploading threads are getting paused by {@link Assembly#pauseUploads()}.
     * @throws IOException - Thrown if an I/O error occurs while loading test resources.
     * @throws LocalOperationException - If a local non-http operation fails.
     * @throws RequestException -  If a request to Transloadit server fails.
     */
    @Test
    public void pauseUploads() throws IOException, LocalOperationException, RequestException {


        String uploadSize = "" + new File("LICENSE").length();
        Assertions.assertEquals("1077", uploadSize);  // verify, that test sizes can work
        String uploadChunkSize = "1";
        assembly = new MockTusAssemblyMultiThreading(transloadit);
        assembly.wipeAssemblyID();
        assembly.setMaxParallelUploads(2);
        assembly.setUploadChunkSize(Integer.parseInt(uploadChunkSize));
        assembly.addFile(new File("LICENSE"), "file_name1");
        assembly.addFile(new File("LICENSE"), "file_name2");


        AssemblyListener listener = getEmptyAssemblyListener();
        AssemblyListener spyListener = Mockito.spy(listener);

        Assembly assemblySpy = Mockito.spy(assembly);
        assemblySpy.setRunnableAssemblyListener(spyListener);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);

        mockServerClient.when(HttpRequest.request()
                        .withPath("/assemblies")
                        .withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0"), Times.once()).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Location", "/resumable/files/a"));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0"), Times.once()).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Location", "/resumable/files/b"));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files/a").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0")
                .withHeader("Upload-Offset", "0")
                .withHeader("X-HTTP-Method-Override", "PATCH")).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", "0"));

        mockServerClient.when(HttpRequest.request()
                .withPath("/resumable/files/b").withMethod("POST").withHeader(
                        "Tus-Resumable", "1.0.0")
                .withHeader("Upload-Offset", "0")
                .withHeader("X-HTTP-Method-Override", "PATCH")).respond(
                new HttpResponse()
                        .withStatusCode(204)
                        .withHeader("Tus-Resumable", "1.0.0")
                        .withHeader("Upload-Offset", "0"));

        assemblySpy.save(true);
        assemblySpy.pauseUploads();
        Mockito.verify(assemblySpy).pauseUploads();
    }

    /**
     * Provides an {@link AssemblyListener} without working callback methods.
     *
     * @return {@link AssemblyListener}
     */
    public AssemblyListener getEmptyAssemblyListener() {
        return new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {

            }

            @Override
            public void onError(Exception error) {

            }

            @Override
            public void onMetadataExtracted() {

            }

            @Override
            public void onAssemblyUploadFinished() {

            }

            @Override
            public void onFileUploadFinished(JSONObject uploadInformation) {

            }

            @Override
            public void onFileUploadPaused(String name) {

            }

            @Override
            public void onFileUploadResumed(String name) {

            }

            @Override
            public void onFileUploadProgress(long uploadedBytes, long totalBytes) {

            }

            @Override
            public void onAssemblyProgress(JSONObject progress) {

            }

            @Override
            public void onAssemblyResultFinished(JSONArray result) {

            }
        };
    }


}

package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
// CHECKSTYLE:OFF
import io.tus.java.client.TusUpload;
// CHECKSTYLE:ON
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Objects;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.RegexBody.regex;

/**
 * Unit test for {@link Assembly} class. Api-Responses are simulated by mocking the server's response.
 */
@ExtendWith(MockServerExtension.class)  // MockServerExtension is used to start and stop the MockServer
@MockServerSettings(ports = MockHttpService.PORT) // MockServerSettings is used to define the port of the MockServer
public class AssemblyTest extends MockHttpService {
    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private final MockServerClient mockServerClient = new MockServerClient("localhost", PORT);

    /**
     * Links to {@link Assembly} instance to perform the tests on.
     */
    private Assembly assembly;

    /**
     * Keeps track of events fired by the {@link AssemblyListener}.
     */
    private final HashMap<String, Integer> emittedEvents = new HashMap<String, Integer>() {{
        put("ASSEMBLY_ERROR", 0);
        put("ASSEMBLY_META_DATA_EXTRACTED", 0);
        put("ASSEMBLY_INSTRUCTION_UPLOAD_FINISHED", 0);
        put("ASSEMBLY_FILE_UPLOAD_FINISHED", 0);
        put("ASSEMBLY_FILE_UPLOAD_PAUSED", 0);
        put("ASSEMBLY_FILE_UPLOAD_RESUMED", 0);
        put("ASSEMBLY_FILE_UPLOAD_PROGRESS", 0);
        put("ASSEMBLY_PROGRESS", 0);
        put("ASSEMBLY_RESULT_FINISHED", 0);
        put("ASSEMBLY_FINISHED", 0);
    }};

    /**
     * Assigns a new {@link Assembly} instance to the {@link AssemblyTest#assembly} variable before each individual test
     * and resets the mockServerClient.
     */
    @BeforeEach
    public void setUp() {
        assembly = newAssemblyWithoutID();

        mockServerClient.reset();
        emittedEvents.replaceAll((key, value) -> 0);
    }

    /**
     * Checks if {@link Assembly#addFile(File, String)} adds the provided files correctly to the {@link Assembly}
     * instance by searching it in the {@link Assembly#files} attribute.
     */
    @Test
    public void addFile() {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        Assertions.assertEquals(file, assembly.files.get("file_name"));
    }

    /**
     * Performs a similar test to {@link Assembly#addFile(File, String)}, except that a FileStream is added.
     * @throws IOException if File cannot be created.
     */
    @Test
    public void addInputStreamFile() throws IOException {
        InputStream file = Files.newInputStream(new File("LICENSE").toPath());
        assembly.addFile(file, "file_name");

        Assertions.assertEquals(file, assembly.fileStreams.get("file_name"));
    }

    /**
     * Performs a double test, adding a file and a FileStream with the same name to the {@link Assembly} instance.
     * Both are added by calling {@link Assembly#addFile}. Test passes if only the latter added {@link File} can be found
     * in the {@link Assembly} instance.
     * @throws IOException if File cannot be created.
     */
    @Test
    public void addCombinedFiles() throws IOException {
        InputStream fileStream = Files.newInputStream(new File("LICENSE").toPath());
        assembly.addFile(fileStream, "file_name");

        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        Assertions.assertFalse(assembly.fileStreams.containsKey("file_name"));
        Assertions.assertEquals(file, assembly.files.get("file_name"));
    }

    /**
     * Performs a test if a file can successfully be removed from an Assembly with {@link Assembly#removeFile(String)}.
     * Therefore, a file gets added to the Assembly and gets removed afterward. Tests passes if the file's name
     * cannot be found in {@link Assembly#files}.
     */
    @Test
    public void removeFile() {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        Assertions.assertTrue(assembly.files.containsKey("file_name"));

        assembly.removeFile("file_name");
        Assertions.assertFalse(assembly.files.containsKey("file_name"));
    }

    /**
     * This Test verifies the HTTP POST - Request sent by the {@link Assembly#save(boolean)} method by proving the
     * request's body content and the request's path. Additional to that this test checks if the corresponding
     * {@link AssemblyResponse} stores the server's response.
     * <ul>
     * <li>The {@link Assembly Assembly's} file(s) are provided as {@link File}.</li>
     * <li>The {@link Assembly#save(boolean)} methods parameter {@code isResumable = false}, indicating that
     * the {@link TusUpload TUS Uploader} should not be used.</li>
     * </ul>
     * @throws IOException if Test resource "assembly_executing.json" is missing.
     * @throws LocalOperationException if building the request goes wrong
     * @throws RequestException if communication with the server goes wrong.
     */
    @Test
    public void save() throws IOException, LocalOperationException, RequestException {
        mockServerClient.when(request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly_executing.json")));

        assembly.addFile(new File("LICENSE"), "file_name");

        AssemblyResponse savedAssembly = assembly.save(false);
        Assertions.assertEquals("ASSEMBLY_EXECUTING", savedAssembly.json().get("ok"));
    }

    /**
     * This test is identical to {@link AssemblyTest#save()} except the {@link Assembly Assembly's} file(s) are provided
     * as {@link FileInputStream}.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resource "assembly.json" is missing.
     */
    @Test
    public void saveWithInputStream() throws Exception {
        mockServerClient.when(request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        assembly.addFile(Files.newInputStream(new File("LICENSE").toPath()), "file_name");

        AssemblyResponse savedAssembly = assembly.save(false);
        Assertions.assertEquals("ASSEMBLY_COMPLETED", savedAssembly.json().get("ok"));
    }

    /**
     * This Test works just like {@link AssemblyTest#save()} but triggers a "wait for assembly completion" behaviour by
     * setting {@link Assembly#shouldWaitForCompletion} = {@code true}.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resources  "assembly_executing.json" or "resumable_assembly_complete.json" are missing.
     * @see Assembly#shouldWaitWithoutSSE()
     */
    @Test
    public void saveTillComplete() throws Exception {
        mockServerClient.when(request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly_executing.json")));

        mockServerClient.when(request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly_complete.json")));

        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.setShouldWaitForCompletion(true);

        AssemblyResponse savedAssembly = assembly.save(false);
        Assertions.assertEquals("ASSEMBLY_COMPLETED", savedAssembly.json().get("ok"));
    }

    /**
     * This test checks the functionality of the {@link Assembly#save(boolean)} method with parameter
     * {@code isResumable = true}, indicating a {@link TusUpload TUSUpload}. The Test passes if the
     * POST - request matches the expected pattern and the corresponding {@link AssemblyResponse} stores the "server's"
     * Response correctly.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resource "resumable_assembly.json" is missing.
     */
    @Test
    public void saveWithTus() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);

        assembly.addFile(new File("LICENSE"), "file_name");

        mockServerClient.when(request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        Assertions.assertEquals("02ce6150ea2811e6a35a8d1e061a5b71", resumableAssembly.json().get("assembly_id"));
        Assertions.assertEquals("ASSEMBLY_UPLOADING", resumableAssembly.json().get("ok"));
    }

    /**
     * This test verifies the functionality of {@link Assembly#save(boolean)}. It is identical to
     * {@link AssemblyTest#saveWithTus()} except it listens to the Socket and waits until the execution of the
     * {@link Assembly} is finished. Therefore, it implements an {@link AssemblyListener} and verifies the needed
     * POST and GET requests to the server.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resources "resumable_assembly.json" or "resumable_assembly_complete.json" are missing.
     */
    @Test
    public void saveWithTusListenSSE() throws Exception {
        String sseBody = getJson("sse_response_body.txt");
        MockTusAssembly assembly = getMockTusAssembly();

        mockServerClient.when(request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        mockServerClient.when(request()
                .withPath("/ws20013").withMethod("GET").withHeader("Accept", "text/event-stream"))
                .respond(HttpResponse.response().withBody(sseBody));

        // When the assembly is finished (finished status)
        mockServerClient.when(request()
                .withPath("/assemblies/02ce6150ea2811e6a35a8d1e061a5b71").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly_complete.json")));

        AssemblyResponse response = assembly.save(true);

        Assertions.assertEquals("ASSEMBLY_UPLOADING", response.json().get("ok"));
        Assertions.assertEquals(0, emittedEvents.get("ASSEMBLY_FINISHED"));

        // Wait for the assembly to finish and the SSE events to be processed
        Thread.sleep(1000);

        // Check if SSE events triggered the correct events and make sure they were triggered often enough:
        Assertions.assertEquals(0, emittedEvents.get("ASSEMBLY_ERROR"));
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_META_DATA_EXTRACTED") >= 1);
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_INSTRUCTION_UPLOAD_FINISHED") >= 1);
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_FILE_UPLOAD_FINISHED") >= 2);
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_PROGRESS") >= 2);
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_RESULT_FINISHED") >= 2);
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_FINISHED") >= 1);

        // We are not doing here actual file uploads, so the next three should not appear:
        Assertions.assertEquals(0, emittedEvents.get("ASSEMBLY_FILE_UPLOAD_PROGRESS"));
        Assertions.assertEquals(0, emittedEvents.get("ASSEMBLY_FILE_UPLOAD_RESUMED"));
        Assertions.assertEquals(0, emittedEvents.get("ASSEMBLY_FILE_UPLOAD_PAUSED"));
    }

    @Test
    public void sseDeliversResultEvenIfFinishedArrivesFirst() throws Exception {
        String originalSse = getJson("sse_response_body.txt");
        String withoutFinish = originalSse.replace("data: assembly_finished\n", "");
        int firstResultIndex = withoutFinish.indexOf("event: assembly_result_finished");
        int secondResultIndex = withoutFinish.indexOf("event: assembly_result_finished", firstResultIndex + 1);
        String finishEvent = "data: assembly_finished\n\n";
        String sseBody;
        if (secondResultIndex >= 0) {
            StringBuilder builder = new StringBuilder(withoutFinish);
            builder.insert(secondResultIndex, finishEvent);
            sseBody = builder.toString();
        } else {
            throw new IllegalStateException("Fixture does not contain two assembly_result_finished events");
        }
        MockTusAssembly assembly = getMockTusAssembly();

        mockServerClient.when(request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        mockServerClient.when(request()
                .withPath("/ws20013").withMethod("GET").withHeader("Accept", "text/event-stream"))
                .respond(HttpResponse.response().withBody(sseBody));

        mockServerClient.when(request()
                .withPath("/assemblies/02ce6150ea2811e6a35a8d1e061a5b71").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly_complete.json")));

        AssemblyResponse response = assembly.save(true);

        Assertions.assertEquals("ASSEMBLY_UPLOADING", response.json().get("ok"));
        Assertions.assertEquals(0, emittedEvents.get("ASSEMBLY_FINISHED"));

        Thread.sleep(1000);

        Assertions.assertEquals(0, emittedEvents.get("ASSEMBLY_ERROR"));
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_RESULT_FINISHED") >= 2,
                "Expected at least two result events (including post-finish), got " + emittedEvents.get("ASSEMBLY_RESULT_FINISHED"));
        Assertions.assertTrue(emittedEvents.get("ASSEMBLY_FINISHED") >= 1,
                "Expected assembly_finished to fire at least once");
    }

    private @NotNull MockTusAssembly getMockTusAssembly() {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.setAssemblyListener(new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {
                increaseEmittedEventCounter("ASSEMBLY_FINISHED");

                Assertions.assertEquals("02ce6150ea2811e6a35a8d1e061a5b71", response.json().get("assembly_id"));
                Assertions.assertEquals("ASSEMBLY_COMPLETED", response.json().get("ok"));
            }

            @Override
            public void onError(Exception error) {
                increaseEmittedEventCounter("ASSEMBLY_ERROR");
            }

            @Override
            public void onMetadataExtracted() {
                increaseEmittedEventCounter("ASSEMBLY_META_DATA_EXTRACTED");
            }

            @Override
            public void onAssemblyUploadFinished() {
                increaseEmittedEventCounter("ASSEMBLY_INSTRUCTION_UPLOAD_FINISHED");
            }

            @Override
            public void onFileUploadFinished(JSONObject uploadInformation) {
                Assertions.assertNotNull(uploadInformation);

                Assertions.assertTrue(uploadInformation.getString("ssl_url")
                        .matches("https://tmp-eu-west-1\\.transloadit\\.net/(\\w{32}/){2}\\w{32}\\.mp3"));
                Assertions.assertTrue(uploadInformation.getString("name").matches(".*\\.mp3"));
                Assertions.assertTrue(uploadInformation.getString("field").matches("file"));

                increaseEmittedEventCounter("ASSEMBLY_FILE_UPLOAD_FINISHED");
            }

            @Override
            public void onFileUploadPaused(String name) {
                // Should not be called in terms of the mock assembly as this is not part of the SSE functionality.
                increaseEmittedEventCounter("ASSEMBLY_FILE_UPLOAD_PAUSED");
            }

            @Override
            public void onFileUploadResumed(String name) {
                // Should not be called in terms of the mock assembly as this is not part of the SSE functionality.
                increaseEmittedEventCounter("ASSEMBLY_FILE_UPLOAD_RESUMED");
            }

            @Override
            public void onFileUploadProgress(long uploadedBytes, long totalBytes) {
                // Should not be called in terms of the mock assembly as this is not part of the SSE functionality.
                increaseEmittedEventCounter("ASSEMBLY_FILE_UPLOAD_PROGRESS");
            }

            @Override
            public void onAssemblyProgress(JSONObject progress) {
                Assertions.assertNotNull(progress);

                int progressCombined = progress.getInt("progress_combined");
                Assertions.assertTrue(String.valueOf(progressCombined).matches("50|100"));

                if (progressCombined < 100) {
                    String progressPerOriginalFile = progress.getJSONArray("progress_per_original_file")
                            .toString().replaceAll("\\s+", "");
                    Assertions.assertTrue(progressPerOriginalFile
                            .matches("\\[(\\{\"progress\":\\d*,\"original_id\":\"\\w{32}\"}.){2}"));
                }

                increaseEmittedEventCounter("ASSEMBLY_PROGRESS");
            }

            @Override
            public void onAssemblyResultFinished(JSONArray result) {
                Assertions.assertNotNull(result);

                String stepName = result.getString(0);
                JSONObject resultMeta = result.getJSONObject(1);

                Assertions.assertEquals("waveformed", stepName);
                Assertions.assertTrue(resultMeta.getString("ssl_url")
                        .matches("https://tmp-eu-west-1\\.transloadit\\.net/(\\w{32}/){2}\\w{32}\\.png"));
                Assertions.assertTrue(resultMeta.getString("mime").matches("image/png"));
                Assertions.assertTrue(resultMeta.getInt("cost") > 0);
                Assertions.assertTrue(resultMeta.getString("id").matches("\\w{32}"));

                increaseEmittedEventCounter("ASSEMBLY_RESULT_FINISHED");
            }

        });
        return assembly;
    }

    /**
     * This Test verifies the functionality of {@link Assembly#save(boolean)}. It is identical to
     * {@link AssemblyTest#saveWithTus()}, except it waits until the {@link Assembly} execution is finished.
     * This is determined by observing the {@link AssemblyResponse} status.
     * @see Assembly#shouldWaitWithoutSSE()
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resources "resumable_assembly.json" or "resumable_assembly_complete.json" are missing.
     */
    @Test
    public void saveWithTusTillComplete() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.setShouldWaitForCompletion(true);

        mockServerClient.when(request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        mockServerClient.when(request()
                .withPath("/assemblies/02ce6150ea2811e6a35a8d1e061a5b71").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly_complete.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        Assertions.assertEquals("02ce6150ea2811e6a35a8d1e061a5b71", resumableAssembly.json().get("assembly_id"));
        Assertions.assertEquals("ASSEMBLY_COMPLETED", resumableAssembly.json().get("ok"));
    }

    /**
     * This test is identical to {@link AssemblyTest#saveWithTus()} except the {@link Assembly Assembly's} files are
     * provided as {@link FileInputStream}.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resource "resumable_assembly.json" is missing.
     */
    @Test
    public void saveWithInputStreamAndTus() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(Files.newInputStream(new File("LICENSE").toPath()), "file_name");

        mockServerClient.when(request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        Assertions.assertEquals("02ce6150ea2811e6a35a8d1e061a5b71", resumableAssembly.json().get("assembly_id"));
    }

    /**
     * Test retry functionality in case of hitting the servers RATE_LIMIT
     * Test passes if two retries are taking place after sending the RATE_LIMIT_REACHED status code 413 gets send to the
     * client.
     * @throws Exception if Test resources are missing or the request cannot be built.
     */
    @Test
    public void testRetryRateLimit() throws Exception {
        int retries = 2;
        // let it retry twice
        mockServerClient.when(request()
                .withPath("/assemblies").withMethod("POST"), Times.exactly(retries))
                .respond(HttpResponse.response().withStatusCode(413).withBody(getJson("rate_limit_reached.json")));

        // let it pass
        mockServerClient.when(request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly_executing.json")));

        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse savedAssembly = assembly.save(false);
        // check if assembly was successfully retried
        Assertions.assertEquals("ASSEMBLY_EXECUTING", savedAssembly.json().get("ok"));
    }


    /**
     * Tests the functionality of {@link Assembly#getClientSideGeneratedAssemblyID()}.
     */
    @Test
    public void getAssemblyId() throws LocalOperationException {
        assembly.setAssemblyId("68fffff5474d40b8bf7a294cfce4aba5");
        Assertions.assertEquals("68fffff5474d40b8bf7a294cfce4aba5", assembly.getClientSideGeneratedAssemblyID());
    }

    /**
     * Tests the integrity check of {@link Assembly#setAssemblyId(String)}.
     */
    @Test
    public void setAssemblyId() throws LocalOperationException {
        String uuid = "6859bd25474d40b8bf7a294cfce4aba5";
        String uuidShort = "6859bd25474d";
        String uuidLong = "6859bd25474d40b8bf7a294cfce4aba56859bd25474d40b8bf7a294cfce4aba5";
        String uuidWrongChar = "6859bd25474d40b8bf-a294cfce4aba5";
        Assertions.assertThrows(LocalOperationException.class, () -> assembly.setAssemblyId(uuidShort));
        Assertions.assertThrows(LocalOperationException.class, () -> assembly.setAssemblyId(uuidWrongChar));
        Assertions.assertThrows(LocalOperationException.class, () -> assembly.setAssemblyId(uuidLong));
        assembly.setAssemblyId(uuid);
        Assertions.assertEquals(uuid, assembly.getClientSideGeneratedAssemblyID());
    }

    /**
     * Tests the functionality of {@link Assembly#generateAssemblyID()}.
     * Test succeeds if a certain pattern is generated and every run generates a different String.
     */
    @Test
    public void generateAssemblyID() {
        String assemblyID1 = assembly.generateAssemblyID();
        String assemblyID2 = assembly.generateAssemblyID();

        Assertions.assertTrue(assemblyID1.matches("[a-f0-9]{32}"));
        Assertions.assertTrue(assemblyID2.matches("[a-f0-9]{32}"));
        Assertions.assertNotEquals(assemblyID1, assemblyID2);
    }

    /**
     * Tests whether the upload Url suffixes are generated correctly.
     */
    @Test
    public void obtainUploadUrlSuffix() throws LocalOperationException {
        Assertions.assertEquals("/assemblies", assembly.obtainUploadUrlSuffix());
        String assemblyID = assembly.generateAssemblyID();
        assembly.setAssemblyId(assemblyID);
        Assertions.assertEquals("/assemblies/" + assemblyID, assembly.obtainUploadUrlSuffix());
    }

    /**
     * Determines if the correct fileSizes are returned by {@link Assembly#getUploadSize()}.
     * @throws IOException if File cannot be created.
     */
    @Test
    public void getUploadSize() throws IOException {
        File file1 = new File(Objects.requireNonNull(getClass().getResource("/__files/assembly_executing.json")).getFile());
        File file2 = new File(Objects.requireNonNull(getClass().getResource("/__files/cancel_assembly.json")).getFile());
        assembly.addFile(file1);
        assembly.addFile(file2);

        long combinedFilesize = Files.size(file1.toPath()) + Files.size(file2.toPath());
        Assertions.assertEquals(combinedFilesize, assembly.getUploadSize());
    }


    /**
     * Determines if correct number of upload files is determined.
     * @throws FileNotFoundException if File cannot be found.
     */
    @Test public void getNumberOfFiles() throws FileNotFoundException {
        File file1 = new File(Objects.requireNonNull(getClass().getResource("/__files/assembly_executing.json")).getFile());
        FileInputStream file2 = new FileInputStream(Objects.requireNonNull(getClass().getResource("/__files/cancel_assembly.json")).getFile());
        assembly.addFile(file1);
        assembly.addFile(file2);

        Assertions.assertEquals(2, assembly.getNumberOfFiles());
    }

    /**
     * Increases the event counter on the specified object key.
     * @param key - Key in @{@link AssemblyTest#emittedEvents}
     */
    public synchronized void increaseEmittedEventCounter(String key) {
        int currentValue = emittedEvents.get(key);
        currentValue += 1;
        emittedEvents.put(key, currentValue);
    }

}

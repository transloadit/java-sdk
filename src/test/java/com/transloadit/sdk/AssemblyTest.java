package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.matchers.Times;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.RegexBody.regex;

/**
 * Unit test for {@link Assembly} class. Api-Responses are simulated by mocking the server's response.
 */
public class AssemblyTest extends MockHttpService {
    /**
     * MockServer can be run using the MockServerRule.
     */
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

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
     * Assings a new {@link Assembly} instance to the {@link AssemblyTest#assembly} variable before each individual test
     * and resets the mockServerClient. Also sets {@link AssemblyTest#assemblyFinished} {@code = false}.
     */
    @Before
    public void setUp() throws Exception {
        assembly = new Assembly(transloadit);
        assemblyFinished = false;
        mockServerClient.reset();
    }

    /**
     * Checks if {@link Assembly#addFile(File, String)} adds the provided files correctly to the {@link Assembly}
     * instance by searching it in the {@link Assembly#files} attribute.
     */
    @Test
    public void addFile() {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertEquals(file, assembly.files.get("file_name"));
    }

    /**
     * Performs a similar test to {@link Assembly#addFile(File, String)}, except that a FileStream is added.
     * @throws Exception if File cannot be created.
     */
    @Test
    public void addInputStreamFile() throws FileNotFoundException {
        InputStream file = new FileInputStream(new File("LICENSE"));
        assembly.addFile(file, "file_name");

        assertEquals(file, assembly.fileStreams.get("file_name"));
    }

    /**
     * Performs a double test, adding a file and a FileStream wtih the same name to the {@link Assembly} instance.
     * Both are added by calling {@link Assembly#addFile}. Test passes if only the later added {@link File} can be found
     * in the {@link Assembly} instance.
     * @throws Exception
     */
    @Test
    public void addCombinedFiles() throws Exception {
        InputStream fileStream = new FileInputStream(new File("LICENSE"));
        assembly.addFile(fileStream, "file_name");

        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertFalse(assembly.fileStreams.containsKey("file_name"));
        assertEquals(file, assembly.files.get("file_name"));
    }

    /**
     * Performs a test if a file can successfully be removed from an Assembly with {@link Assembly#removeFile(String)}.
     * Therefore a file gets added to the Assembly and gets removed afterwards. Tests passes if the file's name
     * cannot be found in {@link Assembly#files}.
     */
    @Test
    public void removeFile() {
        File file = new File("LICENSE");
        assembly.addFile(file, "file_name");

        assertTrue(assembly.files.containsKey("file_name"));

        assembly.removeFile("file_name");
        assertFalse(assembly.files.containsKey("file_name"));
    }

    /**
     * This Test verifies the HTTP POST - Request sent by the {@link Assembly#save(boolean)} method by proving the
     * request's body content and the request's path. Additional to that this test checks if the corresponding
     * {@link AssemblyResponse} stores the server's response.
     * <ul>
     * <li>The {@link Assembly Assembly's} file(s) are provided as {@link File}.</li>
     * <li>The {@link Assembly#save(boolean)} methods parameter {@code isResumable = false}, indicating that
     * the {@link io.tus.java.client.TusUpload TUS Uploader} should not be used.</li>
     * </ul>
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resource "assembly_executing.json" is missing.
     */
    @Test
    public void save() throws IOException, LocalOperationException, RequestException {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly_executing.json")));

        assembly.addFile(new File("LICENSE"), "file_name");

        AssemblyResponse savedAssembly = assembly.save(false);
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_EXECUTING");
    }

    /**
     * This test is identical to {@link AssemblyTest#save()} except the {@link Assembly Assembly's} file(s) are provided
     * as {@link FileInputStream}.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resource "assembly.json" is missing.
     */
    @Test
    public void saveWithInputStream() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));

        assembly.addFile(new FileInputStream(new File("LICENSE")), "file_name");

        AssemblyResponse savedAssembly = assembly.save(false);
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_COMPLETED");
    }

    /**
     * This Test works just like {@link AssemblyTest#save()} but triggers a "wait for assembly completion" behaviour by
     * setting {@link Assembly#shouldWaitForCompletion} = {@code true}.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resources  "assembly_executing.json" or "resumable_assembly_complete.json" are missing.
     * @see Assembly#shouldWaitWithoutSocket()
     */
    @Test
    public void saveTillComplete() throws Exception {
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST")
                // content from the file uploaded is present
                .withBody(regex("[\\w\\W]*Permission is hereby granted, free of charge[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("assembly_executing.json")));

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly_complete.json")));

        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.setShouldWaitForCompletion(true);

        AssemblyResponse savedAssembly = assembly.save(false);
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_COMPLETED");
    }

    /**
     * This test checks the functionality of the {@link Assembly#save(boolean)} method with parameter
     * {@code isResumable = true}, indicating a {@link io.tus.java.client.TusUpload TUSUpload}. The Test passes if the
     * POST - request matches the expected pattern and the corresponding {@link AssemblyResponse} stores the "server's"
     * Response correctly.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resource "resumable_assembly.json" is missing.
     */
    @Test
    public void saveWithTus() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(new File("LICENSE"), "file_name");

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        assertEquals(resumableAssembly.json().get("assembly_id"), "02ce6150ea2811e6a35a8d1e061a5b71");
        assertEquals(resumableAssembly.json().get("ok"), "ASSEMBLY_UPLOADING");
    }

    /**
     * This test verifies the functionality of {@link Assembly#save(boolean)}. It is identical to
     * {@link AssemblyTest#saveWithTus()} except it listens to the Socket and waits until the execution of the
     * {@link Assembly} is finished. Therefore it implements an {@link AssemblyListener} and verifies the needed
     * POST and GET requests to the server.
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resources "resumable_assembly.json" or "resumable_assembly_complete.json" are missing.
     */
    @Test
    public void saveWithTusTillSocketComplete() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.setAssemblyListener(new AssemblyListener() {
            @Override
            public void onAssemblyFinished(AssemblyResponse response) {
                assemblyFinished = true;
                assertEquals(response.json().get("assembly_id"), "02ce6150ea2811e6a35a8d1e061a5b71");
                assertEquals(response.json().get("ok"), "ASSEMBLY_COMPLETED");
            }

            @Override
            public void onError(Exception error) {
                System.err.println("No Mockserver Response");
            }

            @Override
            public void onMetadataExtracted() {

            }

            @Override
            public void onAssemblyUploadFinished() {

            }

            @Override
            public void onFileUploadFinished(String fileName) {

            }

            @Override
            public void onAssemblyResultFinished(AssemblyResponse assemblyResponse) {

            }
        });

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/02ce6150ea2811e6a35a8d1e061a5b71").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly_complete.json")));

        AssemblyResponse response = assembly.save(true);

        assertEquals(response.json().get("ok"), "ASSEMBLY_UPLOADING");
        assertFalse(assemblyFinished);
        assertTrue(assembly.emitted.containsKey("assembly_connect"));
        // emit that assembly is complete
        assembly.getSocket("").emit("assembly_finished");
        assertTrue(assemblyFinished);
    }

    /**
     * This Test verifies the functionality of {@link Assembly#save(boolean)}. It is identical to
     * {@link AssemblyTest#saveWithTus()}, except it waits until the {@link Assembly} execution is finished.
     * This is determined by by observing the {@link AssemblyResponse} status.
     * @see Assembly#shouldWaitWithoutSocket()
     * @throws Exception if communication with the server goes wrong, if building the request goes wrong or
     * if Test resources "resumable_assembly.json" or "resumable_assembly_complete.json" are missing.
     */
    @Test
    public void saveWithTusTillComplete() throws Exception {
        MockTusAssembly assembly = new MockTusAssembly(transloadit);
        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.setShouldWaitForCompletion(true);

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/02ce6150ea2811e6a35a8d1e061a5b71").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly_complete.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        assertEquals(resumableAssembly.json().get("assembly_id"), "02ce6150ea2811e6a35a8d1e061a5b71");
        assertEquals(resumableAssembly.json().get("ok"), "ASSEMBLY_COMPLETED");
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
        assembly.addFile(new FileInputStream(new File("LICENSE")), "file_name");

        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("resumable_assembly.json")));

        AssemblyResponse resumableAssembly = assembly.save(true);
        assertEquals(resumableAssembly.json().get("assembly_id"), "02ce6150ea2811e6a35a8d1e061a5b71");
    }

    /**
     * Test retry functionality in case of hitting the servers RATE_LIMIT
     * Test passes if two retries are taking place after sending the RATE_LIMIT_REACHED status code 413 gets send to the
     * client.
     * @throws Exception if Test resources are missing or the request cannot be built.
     */
    @Test
    public void testRetry() throws Exception {
        int retries = 2;
        // let it retry twice
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"), Times.exactly(retries))
                .respond(HttpResponse.response().withStatusCode(413).withBody(getJson("rate_limit_reached.json")));

        // let it pass
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies").withMethod("POST"))
                .respond(HttpResponse.response().withBody(getJson("assembly_executing.json")));

        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse savedAssembly = assembly.save(false);
        // check if assembly was successfully retried
        assertEquals(savedAssembly.json().get("ok"), "ASSEMBLY_EXECUTING");
    }
}

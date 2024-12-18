package com.transloadit.sdk.async;

//CHECKSTYLE:OFF
// It was necessary to turn off Checkstyle because the import was needed for the links in Javadoc comments,
// but Checkstyle misclassified it as unused.
import com.transloadit.sdk.Assembly;
//CHECKSTYLE:ON
import com.transloadit.sdk.MockHttpService;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;

import static org.mockserver.model.RegexBody.regex;

/**
 * Unit Test class for {@link AsyncAssembly}. Api-Responses are simulated by mocking the server's response.
 */
@ExtendWith(MockServerExtension.class)  // MockServerExtension is used to start and stop the MockServer
@MockServerSettings(ports = MockHttpService.PORT) // MockServerSettings is used to define the port of the MockServer
public class AsyncAssemblyTest extends MockHttpService {
    /**
     * MockServerClient makes HTTP requests to a MockServer instance.
     */
    private final MockServerClient mockServerClient = new MockServerClient("localhost", PORT);

    private AsyncAssembly assembly;
    private AssemblyProgressListener listener;
    private boolean uploadFinished;
    private boolean assemblyFinished;
    private long totalUploaded;
    private Exception statusUpdateError;
    private Exception uploadError;


    /**
     * Sets all variables to the default values before an Assembly execution has taken place.
     * Defines basic Mockserver Expectations to support Assembly creation and status updates.
     * @throws Exception if Test resources "async_resumable_assembly.json" or "assembly.json" are missing.
     */
    @BeforeEach
    public void setUp() throws Exception {
        mockServerClient.reset();
        listener = new Listener();
        assembly = new MockAsyncAssembly(transloadit, listener);
        uploadFinished = false;
        assemblyFinished = false;
        totalUploaded = 0;
        statusUpdateError = null;
        uploadError = null;

        // for assembly creation
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies")
                .withMethod("POST")
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1"
                        + "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("async_resumable_assembly.json")));

        // for assembly status check
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));
    }


    /**
     * This test verifies the functionality of the {@link Assembly#save()} method under the special
     * circumstances of an {@link AsyncAssembly}.
     * @throws LocalOperationException - if local operations are going wrong
     * @throws RequestException - if server communication goes wrong
     * @throws InterruptedException - if an error occurs in thread handling
     */
    @Test
    public void save() throws LocalOperationException, RequestException, InterruptedException {
        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse resumableAssembly = assembly.save();

        synchronized (listener) {
            listener.wait(3000);
        }
        Assertions.assertEquals(resumableAssembly.json().get("assembly_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertTrue(uploadFinished);
        Assertions.assertTrue(assemblyFinished);
        Assertions.assertEquals(1077, totalUploaded);
        Assertions.assertNull(statusUpdateError);
        Assertions.assertNull(uploadError);
    }

    /**
     * This test verifies that uploads are possible even without waiting for their completion.
     * @throws LocalOperationException - if local operations are going wrong
     * @throws RequestException - if server communication goes wrong
     * @throws InterruptedException - if an error occurs in thread handling
     */
    @Test
    public void saveWithoutWaitForCompletion() throws LocalOperationException, RequestException, InterruptedException {
        assembly.addFile(new File("LICENSE"), "file_name");
        assembly.setShouldWaitForCompletion(false);
        AssemblyResponse resumableAssembly = assembly.save();

        synchronized (listener) {
            listener.wait(3000);
        }
        Assertions.assertEquals(resumableAssembly.json().get("assembly_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertTrue(uploadFinished);
        Assertions.assertFalse(assemblyFinished);
        Assertions.assertEquals(1077, totalUploaded);
        Assertions.assertNull(statusUpdateError);
        Assertions.assertNull(uploadError);

        mockServerClient.reset();
    }

    /**
     * This test checks if the error handling works for {@link AsyncAssembly#save(boolean)} method in case of an
     * upload error.
     * @throws LocalOperationException - if local operations are going wrong
     * @throws RequestException - if server communication goes wrong
     * @throws InterruptedException - if an error occurs in thread handling
     */
    @Test
    public void saveWithUploadError() throws LocalOperationException, RequestException, InterruptedException {
        assembly = new MockUploadErrorAsyncAssembly(transloadit, listener);
        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse resumableAssembly = assembly.save();

        synchronized (listener) {
            listener.wait(3000);
        }
        Assertions.assertEquals(resumableAssembly.json().get("assembly_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertFalse(uploadFinished);
        Assertions.assertFalse(assemblyFinished);
        Assertions.assertNotEquals(1077, totalUploaded);
        Assertions.assertNull(statusUpdateError);

        Assertions.assertNotNull(uploadError);
        Assertions.assertEquals("some error message", uploadError.getMessage());
    }

    /**
     * This test checks if the error handling works for {@link AsyncAssembly#save(boolean)} method in case of an
     * status error received from the server.
     * @throws LocalOperationException - if local operations are going wrong
     * @throws RequestException - if server communication goes wrong
     * @throws InterruptedException - if an error occurs in thread handling
     */
    @Test
    public void saveWithStatusError() throws LocalOperationException, RequestException, InterruptedException {
        assembly = new MockStatusErrorAsyncAssembly(transloadit, listener);
        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse resumableAssembly = assembly.save();

        synchronized (listener) {
            listener.wait(3000);
        }
        Assertions.assertEquals(resumableAssembly.json().get("assembly_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertTrue(uploadFinished);
        Assertions.assertEquals(1077, totalUploaded);
        Assertions.assertNull(uploadError);
        Assertions.assertFalse(assemblyFinished);

        Assertions.assertNotNull(statusUpdateError);
        Assertions.assertEquals("some request exception", statusUpdateError.getMessage());
    }

    /**
     * This Test verifies that uploads for {@link AsyncAssembly AsyncAssemblies} can be paused and resumed.
     * @throws LocalOperationException - if local operations are going wrong
     * @throws RequestException - if server communication goes wrong
     * @throws InterruptedException - if an error occurs in thread handling
     */
    @Test
    public void pauseResumeUpload() throws LocalOperationException, RequestException, InterruptedException {
        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse resumableAssembly = assembly.save();

        // ensure that uploading starts before pausing the upload
        synchronized (assembly) {
            assembly.wait(3000);
        }

        // pause the upload
        assembly.pauseUpload();

        // wait for the listener to get triggered. This is expected to timeout, and not be triggered.
        synchronized (listener) {
            listener.wait(3000);
        }
        Assertions.assertEquals(resumableAssembly.json().get("assembly_id"), "76fe5df1c93a0a530f3e583805cf98b4");
        Assertions.assertEquals(MockAsyncAssembly.State.PAUSED, assembly.state);

        // expect the states to not have updated after 5 seconds of wait
        Assertions.assertFalse(uploadFinished);
        Assertions.assertFalse(assemblyFinished);
        Assertions.assertNotEquals(1077, totalUploaded);
        Assertions.assertNull(statusUpdateError);
        Assertions.assertNull(uploadError);

        // resume upload and wait again
        assembly.resumeUpload();
        synchronized (listener) {
            listener.wait(3000);
        }

        // expect the states to have changed as the upload is done this time.
        Assertions.assertTrue(uploadFinished);
        Assertions.assertTrue(assemblyFinished);
        Assertions.assertEquals(1077, totalUploaded);
        Assertions.assertNull(statusUpdateError);
        Assertions.assertNull(uploadError);
    }

    /**
     * Nested class which provides an {@link UploadProgressListener} and {@link AssemblyProgressListener}
     * implementation for jUnit Tests. This Implementation must not be used as productive implementation.
     */
    class Listener implements UploadProgressListener, AssemblyProgressListener {

        /**
         * Always indicates upload has been finished.
         */
        @Override
        public void onUploadFinished() {
            uploadFinished = true;
        }

        /**
         * Sets upload progress to given value.
         * @param uploadedBytes the number of bytes uploaded so far.
         * @param totalBytes the total number of bytes to uploaded (i.e the size of all the files all together).
         */
        @Override
        public void onUploadProgress(long uploadedBytes, long totalBytes) {
            totalUploaded = uploadedBytes;
        }

        /**
         * Always returns {@link AsyncAssemblyTest#assemblyFinished} {@code = true}.
         * @param response {@link AssemblyResponse} response with the updated status of the assembly.
         */
        @Override
        public void onAssemblyFinished(AssemblyResponse response) {
            assemblyFinished = true;
            synchronized (this) {
                notifyAll();
            }
        }

        /**
         * Hands over Upload exception object to {@link AsyncAssemblyTest#uploadError}.
         * @param exception the error that causes the failure.
         */
        @Override
        public void onUploadFailed(Exception exception) {
            uploadError = exception;
            synchronized (this) {
                notifyAll();
            }
        }


        /**
         * Hands over AssemblyStatusUpdate exception object to {@link AsyncAssemblyTest#statusUpdateError}.
         * @param exception the error that causes the failure.
         */
        @Override
        public void onAssemblyStatusUpdateFailed(Exception exception) {
            statusUpdateError = exception;
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public void onParallelUploadsStarting(int parallelUploads, int uploadNumber) {

        }
    }
}

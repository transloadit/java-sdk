package com.transloadit.sdk.async;

import com.transloadit.sdk.MockHttpService;
import com.transloadit.sdk.response.AssemblyResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

import java.io.File;

import static org.junit.Assert.*;
import static org.mockserver.model.RegexBody.regex;

public class AsyncAssemblyTest extends MockHttpService {
    @Rule
    public MockServerRule mockServerRule = new MockServerRule(PORT, this, true);

    private MockServerClient mockServerClient;

    private AsyncAssembly assembly;
    private AssemblyProgressListener listener;
    private boolean uploadFinished;
    private boolean assemblyFinished;
    private long totalUploaded;
    private Exception statusUpdateError;
    private Exception uploadError;


    @Before
    public void setUp() throws Exception {
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
                .withBody(regex("[\\w\\W]*tus_num_expected_upload_files\"\\r\\nContent-Length: 1" +
                        "\\r\\n\\r\\n1[\\w\\W]*")))
                .respond(HttpResponse.response().withBody(getJson("async_resumable_assembly.json")));

        // for assembly status check
        mockServerClient.when(HttpRequest.request()
                .withPath("/assemblies/76fe5df1c93a0a530f3e583805cf98b4").withMethod("GET"))
                .respond(HttpResponse.response().withBody(getJson("assembly.json")));
    }

    @Test
    public void save() throws Exception {
        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse resumableAssembly = assembly.save();

        synchronized (listener) {
            listener.wait(3000);
        }
        assertEquals(resumableAssembly.json().get("id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertTrue(uploadFinished);
        assertTrue(assemblyFinished);
        assertEquals(1077, totalUploaded);
        assertNull(statusUpdateError);
        assertNull(uploadError);
    }

    @Test
    public void saveWithUploadError() throws Exception {
        assembly = new MockUploadErrorAsyncAssembly(transloadit, listener);
        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse resumableAssembly = assembly.save();

        synchronized (listener) {
            listener.wait(3000);
        }
        assertEquals(resumableAssembly.json().get("id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertFalse(uploadFinished);
        assertFalse(assemblyFinished);
        assertNotEquals(1077, totalUploaded);
        assertNull(statusUpdateError);

        assertNotNull(uploadError);
        assertEquals("some error message", uploadError.getMessage());
    }

    @Test
    public void saveWithStatusError() throws Exception {
        assembly = new MockStatusErrorAsyncAssembly(transloadit, listener);
        assembly.addFile(new File("LICENSE"), "file_name");
        AssemblyResponse resumableAssembly = assembly.save();

        synchronized (listener) {
            listener.wait(3000);
        }
        assertEquals(resumableAssembly.json().get("id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertTrue(uploadFinished);
        assertEquals(1077, totalUploaded);
        assertNull(uploadError);
        assertFalse(assemblyFinished);

        assertNotNull(statusUpdateError);
        assertEquals("some request exception", statusUpdateError.getMessage());
    }

    @Test
    public void pauseResumeUpload() throws Exception {
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
        assertEquals(resumableAssembly.json().get("id"), "76fe5df1c93a0a530f3e583805cf98b4");
        assertEquals(MockAsyncAssembly.State.PAUSED, assembly.state);

        // expect the states to not have updated after 5 seconds of wait
        assertFalse(uploadFinished);
        assertFalse(assemblyFinished);
        assertNotEquals(1077, totalUploaded);
        assertNull(statusUpdateError);
        assertNull(uploadError);

        // resume upload and wait again
        assembly.resumeUpload();
        synchronized (listener) {
            listener.wait(3000);
        }

        // expect the states to have changed as the upload is done this time.
        assertEquals(MockAsyncAssembly.State.FINISHED, assembly.state);
        assertTrue(uploadFinished);
        assertTrue(assemblyFinished);
        assertEquals(1077, totalUploaded);
        assertNull(statusUpdateError);
        assertNull(uploadError);
    }

    class Listener implements AssemblyProgressListener {
        @Override
        public void onUploadFinished() {
            uploadFinished = true;
        }

        @Override
        public void onUploadPogress(long uploadedBytes, long totalBytes) {
            totalUploaded = uploadedBytes;
        }

        @Override
        public void onAssemblyFinished(AssemblyResponse response) {
            assemblyFinished = true;
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public void onUploadFailed(Exception exception) {
            uploadError = exception;
            synchronized (this) {
                notifyAll();
            }
        }

        @Override
        public void onAssemblyStatusUpdateFailed(Exception exception) {
            statusUpdateError = exception;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}

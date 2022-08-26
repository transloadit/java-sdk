package com.transloadit.sdk;

import com.transloadit.sdk.async.UploadProgressListener;
import com.transloadit.sdk.response.AssemblyResponse;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusUpload;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

//CHECKSTYLE:OFF
import com.transloadit.sdk.exceptions.LocalOperationException;  // needed for doc
import com.transloadit.sdk.exceptions.RequestException;
import org.json.JSONObject;
//CHECKSTYLE:ON

public class MockProtocolExceptionAssembly extends Assembly {
    private AssemblyListener assemblyListener;
    private long uploadSize;
    private ArrayList<MockTusUploadRunnable> threadList = new ArrayList<MockTusUploadRunnable>();
    private int maxParallelUploads = 2;

    /**
     * Mocks an {@link Assembly} but causes always a {@link ProtocolException} if tus files are getting uploaded.
     * @param transloadit
     */
    public MockProtocolExceptionAssembly(Transloadit transloadit) {
        super(transloadit);
    }

    /**
     * Mocks tus file handling and upload.
     * @throws IOException
     * @throws ProtocolException
     */
    @Override
    protected void uploadTusFiles() throws IOException, ProtocolException {
        if (assemblyListener == null) {
            assemblyListener = new AssemblyListener() {
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
                public void onFileUploadFinished(String fileName, JSONObject uploadInformation) {

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
                public void onAssemblyResultFinished(String stepName, JSONObject result) {

                }
            };
        }
        uploadSize = getUploadSize();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxParallelUploads);
        while (uploads.size() > 0) {
            final TusUpload tusUpload = uploads.remove(0);
            MockTusUploadRunnable tusUploadRunnable = new MockTusUploadRunnable(
                    tusClient, tusUpload, uploadChunkSize, this);
            threadList.add(tusUploadRunnable);
            executor.execute(tusUploadRunnable);
        }
        executor.shutdown();
    }

    /**
     * Calls {@link Assembly#abortUploads()} if called.
     * @param s Thread Name
     * @param e {@link LocalOperationException}
     */
    public void threadThrowsLocalOperationException(String s, Exception e) {
        abortUploads();
    }

    /**
     * Calls {@link Assembly#abortUploads()} if called.
     * @param s Thread Name
     * @param e {@link RequestException}
     */
    public void threadThrowsRequestException(String s, Exception e) {
        abortUploads();
    }
}

class MockTusUploadRunnable extends TusUploadRunnable {
    MockProtocolExceptionAssembly assembly;
    MockTusUploadRunnable(TusClient tusClient, TusUpload tusUpload, int uploadChunkSize, MockProtocolExceptionAssembly
            assembly) {
        super(tusClient, tusUpload, uploadChunkSize, assembly);
        this.assembly = assembly;
    }
    @Override
    public void run() {
        try {
            throw new ProtocolException("AbortUpload");
        } catch (ProtocolException e) {
            assembly.threadThrowsLocalOperationException(this.name, e);
        }
    }
}

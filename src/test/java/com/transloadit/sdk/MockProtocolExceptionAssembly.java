package com.transloadit.sdk;

import com.transloadit.sdk.async.UploadProgressListener;
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
//CHECKSTYLE:ON

public class MockProtocolExceptionAssembly extends Assembly {
    private UploadProgressListener uploadProgressListener;
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
        if (uploadProgressListener == null) {
            uploadProgressListener = new UploadProgressListener() {
                @Override
                public void onUploadFinished() {

                }

                @Override
                public void onUploadProgress(long uploadedBytes, long totalBytes) {

                }

                @Override
                public void onUploadFailed(Exception exception) {

                }

                @Override
                public void onParallelUploadsStarting(int parallelUploads, int uploadNumber) {

                }

                @Override
                public void onParallelUploadsPaused(String name) {

                }

                @Override
                public void onParallelUploadsResumed(String name) {

                }
            };
        }
        uploadSize = getUploadSize();
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxParallelUploads);
        uploadProgressListener.onParallelUploadsStarting(maxParallelUploads, uploads.size());
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
            assembly.threadThrowsRequestException(this.name, e);
        }
    }
}

package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import io.tus.java.client.FingerprintNotFoundException;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.ResumingNotEnabledException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;

import java.io.IOException;


/**
 * This class provides a TusUpload as Thread in order to enable parallel Uploads.
 */
class TusUploadRunnable implements Runnable {
    protected TusUploader tusUploader;
    protected TusUpload tusUpload;
    protected TusClient tusClient;
    protected TusExecutor tusExecutor;
    protected Assembly assembly;

    protected long uploadedBytes = 0;
    protected int uploadChunkSize;
    protected String name;

    protected volatile boolean uploadHasBeenStarted = false;
    protected volatile boolean isUploading = false;
    protected volatile boolean isPaused = false;
    protected volatile boolean isFinishedPermanently = false;
    protected final Object lock;


    /**
     * Constructs an new Instance of the TusUploadThread.
     * @param tusClient Instance of the current {@link TusClient}.
     * @param tusUpload The {@link TusUpload} to be uploaded.
     * @param assembly The calling Assembly instance
     * @param uploadChunkSize The size of an uploadable chunk
     */
   TusUploadRunnable(TusClient tusClient, TusUpload tusUpload, int uploadChunkSize, Assembly assembly) {
        this.tusClient = tusClient;
        this.tusUpload = tusUpload;
        this.assembly = assembly;
        this.uploadChunkSize = uploadChunkSize;
        this.tusExecutor = getTusExecutor();
        this.lock = new Object();

        this.name = "Upload - " + tusUpload.getMetadata().get("filename");
    }

    /**
     * The method to be started by the Task Executor.
     */
    public void run() {
        try {
            this.tusUploader = tusClient.resumeOrCreateUpload(tusUpload);
            this.uploadHasBeenStarted = true;
            if (uploadChunkSize > 0) {
                tusUploader.setChunkSize(uploadChunkSize);
            }
        } catch (ProtocolException | IOException e) {
            assembly.threadThrowsRequestException(this.name, e);
            return;
        }
        this.isUploading = true;
        try {
            tusExecutor.makeAttempts();
        } catch (ProtocolException | IOException e) {
            assembly.threadThrowsRequestException(this.name, e);
        } finally {
            assembly.removeThreadFromList(this);
        }
    }

    /**
     * Returns a {@link TusExecutor} instance, which handles upload coordination.
     * This Executor also handles pause States if it's calling thread is paused.
     * @return {@link TusExecutor}
     */
    private TusExecutor getTusExecutor() {
        return new TusExecutor() {
            @Override
            protected void makeAttempt() throws ProtocolException, IOException {
                int uploadedChunk = 0;
                try {
                    while (uploadedChunk > -1) {
                        if (!isUploading) {
                            isUploading = true;
                        }

                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException("INTERRUPTED");
                        }

                        if (!isPaused) {
                            isUploading = true;
                            uploadedChunk = tusUploader.uploadChunk();
                            if (uploadedChunk > -1) {
                                assembly.updateUploadProgress(uploadedChunk);
                            }
                        } else {
                            if (uploadHasBeenStarted) {
                                //Upload pausing works different if the upload has already benn started
                                synchronized (lock) {
                                    tusUploader.finish(false);
                                    isUploading = false;
                                    assembly.getRunnableAssemblyListener().onFileUploadPaused(name);
                                    lock.wait();
                                }
                            } else {
                                // pauses the Thread even if it has not started the upload.
                                assembly.getRunnableAssemblyListener().onFileUploadPaused(name);
                                lock.wait();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    assembly.threadThrowsLocalOperationException(name, e);
                } finally {
                    isUploading = false;
                    isFinishedPermanently = true;
                    tusUploader.finish();
                }
            }
        };
    }

    /**
     * Sets {@link #isPaused} {@code = true}.
     * This results in pausing the thread after uploading the current chunk.
     * @throws LocalOperationException - If resuming has been disabled.
     */
    public void setPaused() throws LocalOperationException {
        if (!tusClient.resumingEnabled()) {
            throw new LocalOperationException("Resuming has been disabled");
        }
            this.isPaused = true;
    }

    /**
     * Sets {@link #isPaused} {@code = false}.
     * This results in resuming the upload with the next chunk.
     * @throws LocalOperationException - If resuming has been disabled or the upload has not been started.
     * @throws RequestException - If the upload could not be resumed due to a request error
     */
    public void setUnPaused() throws LocalOperationException, RequestException {
        if (uploadHasBeenStarted && !isFinishedPermanently) {  // prohibits an attempt of resuming a finished upload.
            try {
                this.tusUploader = this.tusClient.resumeUpload(tusUpload);
                if (uploadChunkSize > 0) {
                    tusUploader.setChunkSize(uploadChunkSize);
                }
            } catch (FingerprintNotFoundException | ResumingNotEnabledException e) {
                throw new LocalOperationException(e);
            } catch (ProtocolException | IOException e) {
                throw new RequestException(name + " " + e.getMessage());
            }
            this.isPaused = false;
            synchronized (lock) {
                lock.notify();
            }
            assembly.getRunnableAssemblyListener().onFileUploadResumed(this.name);
        }
        if (!isFinishedPermanently && isPaused) {
            this.isPaused = false;
            synchronized (lock) {
                lock.notify();
            }
            assembly.getRunnableAssemblyListener().onFileUploadResumed(this.name);
        }
    }
}


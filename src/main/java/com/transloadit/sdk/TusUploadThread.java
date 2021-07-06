package com.transloadit.sdk;

import io.tus.java.client.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class provides a TusUpload as Thread in order to enable parallel Uploads.
 */
 class TusUploadThread extends Thread {
    private TusUploader tusUploader;
    private TusUpload tusUpload;
    private TusClient tusClient;
    private TusExecutor tusExecutor;

    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private Object lock;

    /**
     * Constructs an new Instance of the TusUploadThread.
     * @param tusClient {@link TusClient} Instance of the current TusClient.
     * @param tusUpload {@link TusUpload} the file to be uploaded.
     */
    public TusUploadThread(TusClient tusClient, TusUpload tusUpload) throws ProtocolException, IOException {
        this.tusClient = tusClient;
        this.tusUpload = tusUpload;
        this.tusUploader = tusClient.resumeOrCreateUpload(tusUpload);
        this.setName("Upload - " + tusUpload.getMetadata().get("filename"));
        this.tusExecutor = getTusExecutor();
        this.lock = new Object();
        //
        tusUploader.setChunkSize(50);

    }

    /**
     * The method to be started by the Task Executor.
     */
    public void run() {
        this.isRunning = true;
        try {
            // todo: remove the timestamp debug code
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime localDateTime = LocalDateTime.now();
            String format = fmt.format(localDateTime);
            System.out.println("Uploadstarted: " +  " Name: " + this.getName() + " Timestamp: "
                    + format + " ChunkSize: " + tusUploader.getChunkSize());
            tusExecutor.makeAttempts();
        } catch (ProtocolException | IOException e) {
            e.printStackTrace();
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
                        if(!isRunning) {
                            isRunning = true;
                        }

                        if (!isPaused) {
                            isRunning = true;
                            uploadedChunk = tusUploader.uploadChunk();
                        } else {
                            synchronized (lock) {
                               // tusUploader.finish();
                                System.out.println(" is Paused");
                                isRunning = false;
                                lock.wait();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                        e.printStackTrace();

                } finally {
                    tusUploader.finish();
                }
            }
        };
    }

    /**
     * Sets {@link #isPaused} {@code = true}.
     * This results in pausing the thread after uploading the current chunk.
     */
    public void setPaused() {
            this.isPaused = true;
    }

    /**
     * Sets {@link #isPaused} {@code = false}.
     * This results in resuming the upload with the next chunk.
     */
    public void setUnPaused() {
            try {
                this.tusUploader = this.tusClient.resumeUpload(tusUpload);
            } catch (ProtocolException | IOException | FingerprintNotFoundException | ResumingNotEnabledException e) {
                e.printStackTrace();
            }

        this.isPaused = false;
            synchronized (lock) {
                lock.notify();
            }
            System.out.println(getName() + " is Resuming ");
        }
}

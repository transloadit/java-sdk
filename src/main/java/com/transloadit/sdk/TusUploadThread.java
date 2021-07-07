package com.transloadit.sdk;

import io.tus.java.client.FingerprintNotFoundException;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.ResumingNotEnabledException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This class provides a TusUpload as Thread in order to enable parallel Uploads.
 */
 class TusUploadThread extends Thread {
    private TusUploader tusUploader;
    private ArrayList<Object> uploadParts;
    private TusUpload tusUpload;
    private TusClient tusClient;
    private TusExecutor tusExecutor;
    private Assembly assembly;
    private Object inputData;
    private String fieldName;
    private String assemblyUrl;

    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private Object lock;

    /**
     * Constructs an new Instance of the TusUploadThread.
     * @param tusClient {@link TusClient} Instance of the current TusClient.
     * @param uploadParts ArrayList that holds the File to be uploaded, its fieldName and the Assembly Url
     * @param assembly Assembly Instance
     */
    TusUploadThread(TusClient tusClient, ArrayList<Object> uploadParts, Assembly assembly)
            throws ProtocolException, IOException {
        this.tusClient = tusClient;
        this.inputData = uploadParts.get(0);
        this.fieldName = (String) uploadParts.get(1);
        this.assemblyUrl = (String) uploadParts.get(2);
        this.tusUpload = makeNewTusUpload();
        this.assembly = assembly;
        this.tusUploader = tusClient.resumeOrCreateUpload(tusUpload);
        this.setName("Upload - " + tusUpload.getMetadata().get("filename"));
        this.tusExecutor = getTusExecutor();
        this.lock = new Object();

        // todo: remove reduced Chunk Size
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
        } finally {
            System.out.println("Thread :" +  this.getName() + "Has finished");
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
                        if(!isRunning) {
                            isRunning = true;
                        }

                        if (!isPaused) {
                            isRunning = true;
                            uploadedChunk = tusUploader.uploadChunk();
                        } else {
                            synchronized (lock) {
                                tusUploader.finish();
                                System.out.println(" is Paused");
                                isRunning = false;
                                lock.wait();
                            }
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    tusUploader.finish();
                }
            }
        };
    }

    private TusUpload makeNewTusUpload() throws IOException {
        TusUpload upload = getTusUploadInstances(inputData, fieldName, assemblyUrl);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", fieldName);
        try {
            File file = (File) inputData;
            metadata.put("filename", file.getName());
        } catch (ClassCastException ignored) { }
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);
        upload.setMetadata(metadata);

        return upload;
    }

    private TusUpload getTusUploadInstances(Object inputData, String fieldName, String assemblyUrl)
            throws IOException {
        TusUpload tusUpload = new TusUpload();
        InputStream inputStream = null;
        try {
            tusUpload.setInputStream((InputStream) inputData);
            inputStream = (InputStream) inputData;
        } catch (ClassCastException ignored) {
        }
        try {
            File file = (File) inputData;
            return new TusUpload(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        tusUpload.setFingerprint(String.format("%s-%d-%s", fieldName, inputStream.available(), assemblyUrl));
        tusUpload.setSize(inputStream.available());

        return tusUpload;
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
                this.tusUpload = makeNewTusUpload();
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

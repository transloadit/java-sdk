package com.transloadit.sdk;

import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class provides a TusUpload as Thread in order to enable parallel Uploads.
 */
class TusUploadThread extends Thread {
    private TusUploader tusUploader;
    private TusUpload tusUpload;
    private TusExecutor tusExecutor;

    /**
     * Constructs an new Instance of the TusUploadThread.
     * @param tusUploader {@link TusUploader} performs the actual file upload.
     * @param tusUpload {@link TusUpload} the file to be uploaded.
     */
    public TusUploadThread(TusUploader tusUploader, TusUpload tusUpload) {
        this.tusUpload = tusUpload;
        this.tusUploader = tusUploader;
        this.setName("Upload - " + tusUpload.getMetadata().get("filename"));
        this.tusExecutor = getTusExecutor();

    }

    /**
     * The method to be started by the Task Executor.
     */
    public void run() {
        try {
            // todo: remove the timestamp debug code
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime localDateTime = LocalDateTime.now();
            String format = fmt.format(localDateTime);
            System.out.println("Uploadstarted: " +  " Name: " + this.getName() + " Timestamp: " + format);

            tusExecutor.makeAttempts();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Returns a {@link TusExecutor} instance, which handles upload coordination.
     * @return {@link TusExecutor}
     */
    private TusExecutor getTusExecutor() {
        TusExecutor tusExecutor = new TusExecutor() {
            @Override
            protected void makeAttempt() throws ProtocolException, IOException {
                int uploadedChunk = 0;
                while (uploadedChunk > -1) {
                    uploadedChunk = tusUploader.uploadChunk();
                }
                tusUploader.finish();
            }
        };

        return tusExecutor;
    }
}

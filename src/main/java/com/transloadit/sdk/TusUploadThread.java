package com.transloadit.sdk;

import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;

import java.io.IOException;

/**
 * This class provides a TusUpload as Thread in order to enable parallel Uploads.
 */
public class TusUploadThread extends Thread {
    private TusUploader tusUploader;
    private TusUpload tusUpload;
    private TusExecutor tusExecutor;

    public TusUploadThread(TusUploader tusUploader, TusUpload tusUpload) {
        this.tusUpload = tusUpload;
        this.tusUploader = tusUploader;
        this.setName("Upload - " + tusUpload.getMetadata().get("filename"));
        this.tusExecutor = getTusExecutor();

    }

    public void run() {
        try {
            tusExecutor.makeAttempts();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

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

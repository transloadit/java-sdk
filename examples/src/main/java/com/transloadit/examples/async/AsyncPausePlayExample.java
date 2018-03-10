package com.transloadit.examples.async;

import com.transloadit.sdk.Transloadit;
import com.transloadit.sdk.async.AssemblyProgressListener;
import com.transloadit.sdk.async.AsyncAssembly;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class AsyncPausePlayExample {
    public static void main(String[] args) throws FileNotFoundException {
        Transloadit transloadit = new Transloadit("TRANSLOADIT_KEY", "TRANSLOADIT_SECRET");

        Map<String, Object> stepOptions = new HashMap<String, Object>();
        stepOptions.put("width", 75);
        stepOptions.put("height", 75);
        stepOptions.put("resize_strategy", "pad");

        ProgressListener listener = new ProgressListener();
        AsyncAssembly assembly = transloadit.newAssembly(listener);
        assembly.addStep("resize", "/image/resize", stepOptions);

        File image = new File(AsyncPausePlayExample.class.getResource("/lol_cat.jpg").getFile());
        assembly.addFile(image);

        try {
            assembly.save();
            // wait the till we are sure that the upload has started
            synchronized (listener) {
                listener.wait(10000);
            }
            System.out.println("about to pause ...");
            assembly.pauseUpload();
            System.out.println("upload just got paused ...");
            Thread.sleep(1000);
            assembly.resumeUpload();
            System.out.println("upload just got resumed ..");
        } catch (RequestException | LocalOperationException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class ProgressListener implements AssemblyProgressListener {
        @Override
        public void onUploadFinished() {
            System.out.println("upload finished!!! waiting for execution ...");
        }

        @Override
        public void onUploadPogress(long uploadedBytes, long totalBytes) {
            double percentage = ((double)uploadedBytes / (double)totalBytes) * 100.0;
            System.out.println("uploaded: " + uploadedBytes + " of: " + totalBytes);

            // notify the main class to pause the upload
            if (percentage > 0 && percentage < 20) {
                synchronized (this) {
                    this.notifyAll();
                }
            }
        }

        @Override
        public void onAssemblyFinished(AssemblyResponse response) {
            System.out.println("Assembly finished with status: " + response.json().getString("ok"));
        }

        @Override
        public void onUploadFailed(Exception exception) {
            System.out.println("upload failed :(");
            exception.printStackTrace();
        }

        @Override
        public void onAssemblyStatusUpdateFailed(Exception exception) {
            System.out.println("unable to fetch status update :(");
            exception.printStackTrace();
        }
    }
}

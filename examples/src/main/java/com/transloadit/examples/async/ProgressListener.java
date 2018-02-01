package com.transloadit.examples.async;

import com.transloadit.sdk.async.AssemblyProgressListener;
import com.transloadit.sdk.response.AssemblyResponse;

public class ProgressListener implements AssemblyProgressListener {
    @Override
    public void onUploadFinished() {
        System.out.println("upload finished!!! waiting for execution ...");
    }

    @Override
    public void onUploadPogress(long uploadedBytes, long totalBytes) {
        System.out.println("uploaded: " + uploadedBytes + " of: " + totalBytes);
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

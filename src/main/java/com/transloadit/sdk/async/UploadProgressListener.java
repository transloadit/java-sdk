package com.transloadit.sdk.async;

/**
 * Implementations of this interface are used to handle progress and completeion of a background
 * Assembly file upload.
 */
public interface UploadProgressListener {

    /**
     * Callback to be executed when the Assembly upload is complete.
     */
    void onUploadFinished();

    /**
     * Callback to be executed as an upload progress receiver.
     *
     * @param uploadedBytes the number of bytes uploaded so far.
     * @param totalBytes the total number of bytes to uploaded (i.e the size of all the files all together).
     */
    void onUploadProgress(long uploadedBytes, long totalBytes);

    /**
     * Callback to be executed if the Assembly upload fails.
     *
     * @param exception the error that causes the failure.
     */
    void onUploadFailed(Exception exception);
}

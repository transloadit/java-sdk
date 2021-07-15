package com.transloadit.sdk.async;

/**
 * Implementations of this interface are used to handle progress and completion of a background
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

    /**
     * Callback to be executed if parallel uploads are starting.
     * @param parallelUploads Inidcates how many files are uploaded in parallel
     * @param uploadNumber  Indicates how many files are in the upload Queue;
     */
    void onParallelUploadsStarting(int parallelUploads, int uploadNumber);

    /**
     * Callback to be executed if an already running, parallel upload gets paused.
     * @param name Name of the upload Thread, which gets paused.
     */
    void onParallelUploadsPaused(String name);

    /**
     * Callback to be executed if an already running, parallel upload gets paused.
     * @param name Name of the upload Thread, which gets paused.
     */
    void onParallelUploadsResumed(String name);
}

package com.transloadit.sdk.async;

/**
 * Deprecated as being a part of {@link AsyncAssembly}
 * Implementations of this interface are used to handle progress and completion of a background
 * Assembly file upload.
 */
@Deprecated
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
     * Callback to be executed if the Assembly uploads are starting.
     *
     * @param parallelUploads Number of started uploads.
     * @param uploadNumber Number of the specific started upload.
     */
    void onParallelUploadsStarting(int parallelUploads, int uploadNumber);
}

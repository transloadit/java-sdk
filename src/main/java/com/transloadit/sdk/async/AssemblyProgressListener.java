package com.transloadit.sdk.async;

import com.transloadit.sdk.response.AssemblyResponse;


/**
 * Deprecated because its use is limited to the also deprecated {@link AsyncAssembly}.
 * Implementations of this interface are used to handle progress and completion of a background
 * Assembly file upload and execution.
 */
@Deprecated
public interface AssemblyProgressListener {

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
     * Callback to be executed when the Assembly execution is done executing.
     * This encompasses any kind of termination of the assembly.
     * Including when the assembly aborts due to failure.
     *
     * @param response {@link AssemblyResponse} response with the updated status of the assembly.
     */
    void onAssemblyFinished(AssemblyResponse response);

    /**
     * Callback to be executed if the Assembly upload fails.
     *
     * @param exception the error that causes the failure.
     */
    void onUploadFailed(Exception exception);

    /**
     * Callback to be executed if the Assembly status update retrieve fails.
     *
     * @param exception the error that causes the failure.
     */
    void onAssemblyStatusUpdateFailed(Exception exception);
}

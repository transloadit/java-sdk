package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Interface for a Listener, which tracks the progress in Assembly execution.
 */
public interface AssemblyListener {
    /**
     * Callback to be executed when the Assembly execution is done executing.
     * This encompasses any kind of termination of the assembly.
     * Including when the assembly aborts due to failure.
     *
     * @param response {@link AssemblyResponse} response with the updated status of the assembly.
     */
    void onAssemblyFinished(AssemblyResponse response);

    /**
     * Callback to be executed if the socket connection throws an Error.
     * This encompasses any kind of termination of the assembly.
     *
     * @param error {@link Exception} the error thrown during the socket connection.
     */
    void onError(Exception error);

    /**
     * Callback to be exectued if the Assembly's files metadata has been extracted.
     */
    void onMetadataExtracted();

    /**
     * Callback to be executed if the Assembly's files have been uploaded and the Assembly execution starts.
     */
    void onAssemblyUploadFinished();

    /**
     * Callback to be executed if one of the Assembly's files has been uploaded.
     * @param uploadInformation {@link JSONObject}, which holds information about the uploaded file as Key-Value pairs.
     */
    void onFileUploadFinished(JSONObject uploadInformation);

    /**
     * Callback to be executed if an already running, parallel upload gets paused.
     * @param name Name of the upload Thread, which gets paused.
     */
    void onFileUploadPaused(String name);

    /**
     * Callback to be executed if an already running, parallel upload gets paused.
     * @param name Name of the upload Thread, which gets paused.
     */
    void onFileUploadResumed(String name);
    /**
     * Callback to be executed as an upload progress receiver.
     *
     * @param uploadedBytes the number of bytes uploaded so far.
     * @param totalBytes the total number of bytes to uploaded (i.e the size of all the files all together).
     */
    void onFileUploadProgress(long uploadedBytes, long totalBytes);

    /**
     * Callback to be executed if am assembly execution progress is propagated from the backend.
     *
     * @param progress JSONObject containing the progress information, pushed from the backend.
     */
    void onAssemblyProgress(JSONObject progress);

    /**
     * Callback to be executed if there is an Assembly result.
     * @param result {@link JSONObject} which holds information about the result as Key-Value pairs.
     */
    void onAssemblyResultFinished(JSONArray result);




}

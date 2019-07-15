package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;

public interface AssemblyListener {
    /**
     * Callback to be executed when the Assembly execution is done executing.
     * This encompasses any kind of termination of the assembly.
     * Including when the assembly aborts due to failure.
     *
     * @param response {@link AssemblyResponse} response with the updated status of the assembly.
     */
    void onAssemblyFinished(AssemblyResponse response);

    void onError(Exception error);
}

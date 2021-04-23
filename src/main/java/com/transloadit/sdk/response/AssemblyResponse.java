package com.transloadit.sdk.response;

import com.transloadit.sdk.exceptions.LocalOperationException;
import e, false);
    }

    /**
     *
     * @return assembly id
     */
    public String getId() {
        return this.json().getString("assembly_id");
    }

    /**
     *
    /**
     *
     * @return assembly ssl url
     */
    public String getSslUrl() {
        return this.json().getString("assembly_ssl_url");
    }

    /**
     *
     * @return assembly websocket url
     */
    public String getWebsocketUrl() {
        return this.json().getString("websocket_url");
    }

    /**
     * returns the assembly result of a particular step.
     *
     * @param stepName the name of the step.
     * @return {@link JSONArray} the assembly result of the specified step.
     */
    public JSONArray getStepResult(String stepName) {
        return json().getJSONObject("results").getJSONArray(stepName);
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has completed.
     */
    public Boolean isCompleted() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_COMPLETED");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has aborted.
     */
    public Boolean isAborted() {
        return json().has("ok") && json().getString("ok").equals("REQUEST_ABORTED");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has canceled.
     */
    public Boolean isCanceled() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_CANCELED");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly is still executing.
     */
    public Boolean isExecuting() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_EXECUTING");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly is uploading.
     */
    public Boolean isUploading() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_UPLOADING");
    }

    /**
     *
     * @return {@link Boolean} indicating if the assembly returned an error
     */
    public Boolean hasError() {
        return json().has("error");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has stopped executing.
     */
    public Boolean isFinished() {
        return !isUploading() && !isExecuting();
    }
}

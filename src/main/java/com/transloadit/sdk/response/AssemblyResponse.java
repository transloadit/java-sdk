package com.transloadit.sdk.response;

import com.transloadit.sdk.exceptions.LocalOperationException;
import org.json.JSONArray;

/**
 * An AssemblyApi tailored Http Response.
 */
public class AssemblyResponse extends Response {
    /**
     * Initializes a new AssemblyResponse object representing the response of the AssemblyApi.
     * @param response {@link okhttp3.Response} - response from interaction with the Web API
     * @param usesTus indicates if {@link io.tus.java.client.TusClient} is used
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public AssemblyResponse(okhttp3.Response response, boolean usesTus) throws LocalOperationException {
        super(response);
    }

    /**
     * Calls {@link #AssemblyResponse(okhttp3.Response, boolean)} with boolean usesTus = false.
     * @param response {@link okhttp3.Response} - response from interaction with the Web API
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public AssemblyResponse(okhttp3.Response response) throws LocalOperationException {
        this(response, false);
    }

    /**
     * Returns the ID of the current Assembly
     * @return assembly id
     */
    public String getId() {
        return this.json().getString("assembly_id");
    }

    /**
     * Returns the URL of the current Assembly whilst using TUS.
     * @return assembly url
     */
    public String getTusUrl() {
        return this.json().getString("tus_url");
    }


    /**
     * Returns the URL of the current Assembly
     * @return assembly url
     */
    public String getUrl() {
        return this.json().getString("assembly_url");
    }

    /**
     * Returns the URL of the current Assembly with the "https://" prefix.
     * @return assembly ssl url
     */
    public String getSslUrl() {
        return this.json().getString("assembly_ssl_url");
    }

    /**
     * Returns the URL of the websocket used in the Assembly execution.
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
     * Checks the execution status of the current Assembly for completion.
     * @return {@link Boolean} indicating the assembly has completed.
     */
    public Boolean isCompleted() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_COMPLETED");
    }

    /**
     * Checks the execution status of the current Assembly for abortion.
     * @return {@link Boolean} indicating the assembly has aborted.
     */
    public Boolean isAborted() {
        return json().has("ok") && json().getString("ok").equals("REQUEST_ABORTED");
    }

    /**
     * Checks the execution status of the current Assembly for being canceled.
     * @return {@link Boolean} indicating the assembly has canceled.
     */
    public Boolean isCanceled() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_CANCELED");
    }

    /**
     * Checks the execution status of the current Assembly for being still in execution.
     * @return {@link Boolean} indicating the assembly is still executing.
     */
    public Boolean isExecuting() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_EXECUTING");
    }

    /**
     * Checks the execution status of the current Assembly for being in the upload process.
     * @return {@link Boolean} indicating the assembly is uploading.
     */
    public Boolean isUploading() {
        return json().has("ok") && json().getString("ok").equals("ASSEMBLY_UPLOADING");
    }

    /**
     * Checks the execution status of the current Assembly for errors.
     * @return {@link Boolean} indicating if the assembly returned an error
     */
    public Boolean hasError() {
        return json().has("error");
    }

    /**
     * Checks if the execution of the current Assembly has been finished.
     * @return {@link Boolean} indicating the assembly has stopped executing.
     */
    public Boolean isFinished() {
        return !isUploading() && !isExecuting();
    }
}

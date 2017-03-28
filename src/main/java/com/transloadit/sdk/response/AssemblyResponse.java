package com.transloadit.sdk.response;

import com.transloadit.sdk.exceptions.LocalOperationException;

/**
 * An AssemblyApi tailored Http Response
 */
public class AssemblyResponse extends Response {
    protected boolean usesTus;

    public AssemblyResponse(okhttp3.Response response, boolean usesTus) throws LocalOperationException {
        super(response);
        this.usesTus = usesTus;
    }

    public AssemblyResponse(okhttp3.Response response) throws LocalOperationException {
        this(response, false);
    }

    /**
     *
     * @return assembly id
     */
    public String getId() {
        return this.json().getString( usesTus ? "id" : "assembly_id");
    }

    /**
     *
     * @return assembly url
     */
    public String getUrl() {
        return this.json().getString(usesTus ? "status_endpoint" : "assembly_url");
    }

    /**
     *
     * @return assembly ssl url
     */
    public String getSslUrl() {
        return this.json().getString(usesTus ? "status_endpoint" : "assembly_ssl_url");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has completed.
     */
    public Boolean isCompleted() {
        return json().getString("ok").equals("ASSEMBLY_COMPLETED");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has aborted.
     */
    public Boolean isAborted() {
        return json().getString("ok").equals("REQUEST_ABORTED");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has canceled.
     */
    public Boolean isCanceled() {
        return json().getString("ok").equals("ASSEMBLY_CANCELED");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly is still executing.
     */
    public Boolean isExecuting() {
        return json().getString("ok").equals("ASSEMBLY_EXECUTING");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly is uploading.
     */
    public Boolean isUploading() {
        return json().getString("ok").equals("ASSEMBLY_UPLOADING");
    }

    /**
     *
     * @return {@link Boolean} indicating the assembly has stopped executing.
     */
    public Boolean isFinished() {
        return isAborted() || isCanceled() || isCompleted();
    }
}

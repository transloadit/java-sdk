package com.transloadit.sdk.response;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.transloadit.sdk.TransloaditRequestException;

/**
 * An AssemblyApi tailored Http Response
 */
public class AssemblyResponse extends Response {
    public final String id;
    public final String url;
    public final String sslUrl;

    public AssemblyResponse(HttpResponse<JsonNode> response) {
        super(response);
        id = this.json().getString("assembly_id");
        url = this.json().getString("assembly_url");
        sslUrl = this.json().getString("assembly_ssl_url");
    }

    /**
     * reloads the assemblyApi to get its updated status.
     */
    public void reload() throws TransloaditRequestException {
        try {
            httpResponse = Unirest.get(url).asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    /**
     * cancels the execution of the assemblyApi.
     */
    public void cancel() throws TransloaditRequestException{
        try {
            httpResponse = Unirest.delete(url).asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
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
     * @return {@link Boolean} indicating the assembly has stopped executing.
     */
    public Boolean isFinished() {
        return isAborted() || isCanceled() || isCompleted();
    }
}

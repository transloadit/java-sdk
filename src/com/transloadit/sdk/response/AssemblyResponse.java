package com.transloadit.sdk.response;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * An Assembly tailored Http Response
 *
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
     * reloads the assembly to get its updated status
     */
    public void reload() {
        try {
            httpResponse = Unirest.get(url).asJson();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * cancels the execution of the assembly.
     */
    public void cancel() {
        try {
            httpResponse = Unirest.delete(url).asJson();
        } catch (UnirestException e) {
            throw new RuntimeException(e);
        }
    }
}

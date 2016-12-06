package com.transloadit.sdk.response;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONObject;

/**
 * Transloadit Api Response class
 */
public class Response {
    protected HttpResponse<JsonNode> httpResponse;

    public Response(HttpResponse<JsonNode> response) {
        httpResponse = response;
    }

    /**
     *
     * @return the json content of the response as an instance of ({@link JSONObject})
     */
    public JSONObject json(){
        return httpResponse.getBody().getObject();
    }
}

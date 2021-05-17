package com.transloadit.sdk.response;

import com.transloadit.sdk.exceptions.LocalOperationException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Transloadit Api Response class.
 */
public class Response {
    private okhttp3.Response httpResponse;
    private String stringBody;

    /**
     * Initializes a new Response object for a Http Response from the Transloadit Api.
     * @param response {@link okhttp3.Response} - response from interaction with Web API
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public Response(okhttp3.Response response) throws LocalOperationException {
        httpResponse = response;
        try {
            stringBody = response.body().string();
        } catch (IOException e) {
            throw new LocalOperationException(e);
        }
    }

    /**
     *
     * @return the json content of the response as an instance of ({@link JSONObject})
     */
    public JSONObject json() {
        return new JSONObject(stringBody);
    }

    /**
     *
     * @return http status code.
     */
    public int status() {
        return httpResponse.code();
    }
}

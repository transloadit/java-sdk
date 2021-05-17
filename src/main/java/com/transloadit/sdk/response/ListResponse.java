package com.transloadit.sdk.response;

import com.transloadit.sdk.exceptions.LocalOperationException;
import org.json.JSONArray;

/**
 * A List tailored Http Response.
 */
public class ListResponse extends Response {
    /**
     * Initializes a new instance of a Http-response suitable for list operations.
     * @param response {@link okhttp3.Response} - response from interaction with the Web API
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public ListResponse(okhttp3.Response response) throws LocalOperationException {
        super(response);
    }

    /**
     *
     * @return {@link JSONArray} iterable of items on the list
     */
    public JSONArray getItems() {
        return json().getJSONArray("items");
    }

    /**
     *
     * @return the number items on the list
     */
    public int size() {
        return json().getInt("count");
    }
}

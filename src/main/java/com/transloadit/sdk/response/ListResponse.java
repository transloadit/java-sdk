package com.transloadit.sdk.response;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONArray;

/**
 * A List tailored Http Response
 */
public class ListResponse extends Response {
    public ListResponse(HttpResponse<JsonNode> response) {
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

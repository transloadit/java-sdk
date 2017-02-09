package com.transloadit.sdk.response;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.json.JSONArray;

/**
 * A List tailored Http Response
 */
public class ListResponse extends Response {
    public final JSONArray items;
    public final int size;

    public ListResponse(HttpResponse<JsonNode> response) {
        super(response);
        items = json().getJSONArray("items");
        size = json().getInt("count");
    }
}

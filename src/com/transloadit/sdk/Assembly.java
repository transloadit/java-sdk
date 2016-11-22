package com.transloadit.sdk;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.Map;

/**
 * Created by ifedapo on 17/11/2016.
 */
public class Assembly {
    public Transloadit transloadit;

    public Assembly(Transloadit transloadit) {
        this.transloadit = transloadit;
    }

    public HttpResponse<JsonNode> list(Map options) throws UnirestException {
        Request request = new Request(transloadit);
        return request.get("http://api2.transloadit.com/assemblies", options);
    }

    public void create() {

    }

}

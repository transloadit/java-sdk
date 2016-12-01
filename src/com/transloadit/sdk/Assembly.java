package com.transloadit.sdk;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.File;
import java.util.HashMap;
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
        return request.get("/assemblies", options);
    }

    public HttpResponse<JsonNode> create(Map options, File[] files) throws UnirestException {
        Map extraData = new HashMap();
        for (int i = 0; i < files.length; i++) {
            extraData.put("file_" + i, files[i]);
        }
        Request request = new Request(transloadit);
        return request.post("/assemblies", options, extraData);
    }

    public HttpResponse<JsonNode> get(String id) throws UnirestException {
        Request request = new Request(transloadit);
        return request.get("/assemblies/" + id, new HashMap());
    }

}

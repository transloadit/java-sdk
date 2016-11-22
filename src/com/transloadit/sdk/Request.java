package com.transloadit.sdk;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ifedapo on 21/11/2016.
 */
public class Request {
    public Transloadit transloadit;

    public Request(Transloadit transloadit) {
        this.transloadit = transloadit;
    }

    public HttpResponse<JsonNode> post(String url, Map data) throws UnirestException {
        return Unirest.post(url)
                .body(getPayload(data))
                .asJson();
    }

    public HttpResponse<JsonNode> get(String url, Map data) throws UnirestException {
        data.put("auth", getAuthData());

        return Unirest.get(url)
                .queryString("signature", getSignature(data))
                .queryString("params", jsonifyData(data))
                .asJson();
    }

    public HttpResponse<JsonNode> delete(String url, Map data) throws UnirestException {
        return Unirest.delete(url)
                .body(getPayload(data))
                .asJson();
    }

    public HttpResponse<JsonNode> put(String url, Map data) throws UnirestException {
        return Unirest.put(url)
                .body(getPayload(data))
                .asJson();
    }

    private String getPayload(Map data) {
        data.put("auth", getAuthData());
        data.put("signature", getSignature(data));

        return jsonifyData(data);
    }

    private String jsonifyData(Map data) {
        JSONObject jsonData = new JSONObject(data);
        String stringData = jsonData.toString();

        return stringData;
    }

    private Map getAuthData() {
        Map auth = new HashMap();
        auth.put("key", transloadit.key);
        auth.put("expires", "2017/11/21 01:01:20+00:00");

        return auth;
    }

    private String getSignature(Map data) {
        return transloadit.getSignatureKey(jsonifyData(data));
    }
}

package com.transloadit.sdk;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Transloadit tailored Http Request class
 */
public class Request {
    public Transloadit transloadit;
    public static final String BASEURL = "http://api2.transloadit.com";

    public Request(Transloadit transloadit) {
        this.transloadit = transloadit;
    }


    public HttpResponse<JsonNode> get(String url, Map<String, Object> data) throws UnirestException {
        return Unirest.get(BASEURL + url)
                .queryString(toPayload(data))
                .asJson();
    }

    public HttpResponse<JsonNode> get(String url) throws UnirestException {
        return get(url, new HashMap<>());
    }

    public HttpResponse<JsonNode> post(String url, Map<String, Object> data) throws UnirestException {
        return post(url, data, new HashMap<>());
    }

    public HttpResponse<JsonNode> post(String url, Map<String, Object> data, Map<String, Object> extraData) throws UnirestException {
        Map<String, Object> payload = toPayload(data);
        payload.putAll(extraData);

        return Unirest.post(BASEURL + url)
                .fields(payload)
                .asJson();
    }

    public HttpResponse<JsonNode> delete(String url, Map<String, Object> data) throws UnirestException {
        return Unirest.delete(BASEURL + url)
                .fields(toPayload(data))
                .asJson();
    }

    public HttpResponse<JsonNode> put(String url, Map<String, Object> data) throws UnirestException {
        return Unirest.put(BASEURL + url)
                .fields(toPayload(data))
                .asJson();
    }

    private Map<String, Object> toPayload(Map<String, Object> data) {
        Map<String, Object> dataClone = new HashMap<>(data);
        dataClone.put("auth", transloadit.getAuthData());

        Map<String, Object> payload = new HashMap<>();
        payload.put("params", jsonifyData(dataClone));
        payload.put("signature", getSignature(dataClone));

        return payload;
    }

    private String jsonifyData(Map<String, Object> data) {
        JSONObject jsonData = new JSONObject(data);

        return jsonData.toString();
    }

    private String getSignature(Map<String, Object> data) {
        return transloadit.getSignature(jsonifyData(data));
    }
}

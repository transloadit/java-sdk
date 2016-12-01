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
    public static final String BASEURL = "http://api2.transloadit.com";

    public Request(Transloadit transloadit) {
        this.transloadit = transloadit;
    }


    public HttpResponse<JsonNode> get(String url, Map data) throws UnirestException {
        return Unirest.get(BASEURL + url)
                .queryString(toPayload(data))
                .asJson();
    }

    public HttpResponse<JsonNode> post(String url, Map data) throws UnirestException {
        return Unirest.post(BASEURL + url)
                .fields(toPayload(data))
                .asJson();
    }

    public HttpResponse<JsonNode> post(String url, Map data, Map extraData) throws UnirestException {
        Map payload = toPayload(data);
        payload.putAll(extraData);

        return Unirest.post(BASEURL + url)
                .fields(payload)
                .asJson();
    }

    public HttpResponse<JsonNode> delete(String url, Map data) throws UnirestException {
        return Unirest.delete(BASEURL + url)
                .body(toPayload(data))
                .asJson();
    }

    public HttpResponse<JsonNode> put(String url, Map data) throws UnirestException {
        return Unirest.put(BASEURL + url)
                .body(toPayload(data))
                .asJson();
    }

    private Map toPayload(Map data) {
        Map dataClone = new HashMap(data);
        dataClone.put("auth", transloadit.getAuthData());

        Map payload = new HashMap();
        payload.put("params", jsonifyData(dataClone));
        payload.put("signature", getSignature(dataClone));

        return payload;
    }

    private String jsonifyData(Map data) {
        JSONObject jsonData = new JSONObject(data);
        String stringData = jsonData.toString();

        return stringData;
    }

    private String getSignature(Map data) {
        return transloadit.getSignature(jsonifyData(data));
    }
}

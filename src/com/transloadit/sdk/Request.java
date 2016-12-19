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
    public static final String BASEURL = "https://api2.transloadit.com";

    Request(Transloadit transloadit) {
        this.transloadit = transloadit;
    }


    HttpResponse<JsonNode> get(String url, Map<String, Object> data) throws TransloaditRequestException {
        try {
            return Unirest.get(BASEURL + url)
                    .queryString(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    HttpResponse<JsonNode> get(String url) throws TransloaditRequestException {
        return get(url, new HashMap<>());
    }

    HttpResponse<JsonNode> post(String url, Map<String, Object> data) throws TransloaditRequestException {
        return post(url, data, new HashMap<>());
    }

    HttpResponse<JsonNode> post(String url, Map<String, Object> data, Map<String, Object> extraData) throws TransloaditRequestException {
        Map<String, Object> payload = toPayload(data);
        payload.putAll(extraData);

        try {
            return Unirest.post(BASEURL + url)
                    .fields(payload)
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    HttpResponse<JsonNode> delete(String url, Map<String, Object> data) throws TransloaditRequestException {
        try {
            return Unirest.delete(BASEURL + url)
                    .fields(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    HttpResponse<JsonNode> put(String url, Map<String, Object> data) throws TransloaditRequestException {
        try {
            return Unirest.put(BASEURL + url)
                    .fields(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
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

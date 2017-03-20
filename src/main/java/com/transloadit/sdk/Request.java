package com.transloadit.sdk;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Transloadit tailored Http Request class
 */
public class Request {
    private Transloadit transloadit;

    Request(Transloadit transloadit) {
        this.transloadit = transloadit;

        Unirest.setDefaultHeader("User-Agent", "Transloadit Java SDK");
    }

    HttpResponse<JsonNode> get(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {
        try {
            return Unirest.get(getFullUrl(url))
                    .queryString(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new RequestException(e);
        }
    }

    HttpResponse<JsonNode> get(String url) throws RequestException, LocalOperationException {
        return get(url, new HashMap<String, Object>());
    }

    HttpResponse<JsonNode> post(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {
        return post(url, data, new HashMap<String, Object>());
    }

    HttpResponse<JsonNode> post(String url, Map<String, Object> data, Map<String, Object> extraData)
            throws RequestException, LocalOperationException {
        Map<String, Object> payload = toPayload(data);
        payload.putAll(extraData);

        try {
            return Unirest.post(getFullUrl(url))
                    .fields(payload)
                    .asJson();
        } catch (UnirestException e) {
            throw new RequestException(e);
        }
    }

    HttpResponse<JsonNode> delete(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {
        try {
            return Unirest.delete(getFullUrl(url))
                    .fields(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new RequestException(e);
        }
    }

    HttpResponse<JsonNode> put(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {
        try {
            return Unirest.put(getFullUrl(url))
                    .fields(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new RequestException(e);
        }
    }

    private String getFullUrl(String url) {
        return url.startsWith("https://") || url.startsWith("http://") ? url : transloadit.getHostUrl() + url;
    }

    private Map<String, Object> toPayload(Map<String, Object> data) throws LocalOperationException {
        Map<String, Object> dataClone = new HashMap<String, Object>(data);
        dataClone.put("auth", getAuthData());

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("params", jsonifyData(dataClone));
        payload.put("signature", getSignature(jsonifyData(dataClone)));

        return payload;
    }

    private String jsonifyData(Map<String, Object> data) {
        JSONObject jsonData = new JSONObject(data);

        return jsonData.toString();
    }

    /**
     *
     * @return Map containing authentication key and the time it expires
     */
    private Map<String, String> getAuthData() {
        Map<String, String> authData = new HashMap<String, String>();
        authData.put("key", transloadit.key);

        Instant expiryTime = Instant.now().plus(transloadit.duration * 1000);
        DateTimeFormatter formatter = DateTimeFormat
                .forPattern("Y/MM/dd HH:mm:ss+00:00")
                .withZoneUTC();

        authData.put("expires", formatter.print(expiryTime));

        return authData;
    }

    /**
     *
     * @param message String data that needs to be encrypted.
     * @return signature generate based on the message passed and the transloadit secret.
     */
    private String getSignature(String message) throws LocalOperationException {
        byte[] kSecret = transloadit.secret.getBytes(Charset.forName("UTF-8"));
        byte[] rawHmac = HmacSHA1(kSecret, message);
        byte[] hexBytes = new Hex().encode(rawHmac);

        return new String(hexBytes, Charset.forName("UTF-8"));
    }

    private byte[] HmacSHA1(byte[] key, String data) throws LocalOperationException {
        final String ALGORITHM = "HmacSHA1";
        Mac mac;

        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
        } catch (NoSuchAlgorithmException e) {
            throw new LocalOperationException(e);
        } catch (InvalidKeyException e) {
            throw new LocalOperationException(e);
        }
        return mac.doFinal(data.getBytes(Charset.forName("UTF-8")));
    }
}

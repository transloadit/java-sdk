package com.transloadit.sdk;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Transloadit tailored Http Request class
 */
public class Request {
    public Transloadit transloadit;

    Request(Transloadit transloadit) {
        this.transloadit = transloadit;

        Unirest.setDefaultHeader("User-Agent", "Transloadit Java SDK");
    }

    HttpResponse<JsonNode> get(String url, Map<String, Object> data)
            throws TransloaditRequestException, TransloaditSignatureException {
        try {
            return Unirest.get(getFullUrl(url))
                    .queryString(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    HttpResponse<JsonNode> get(String url) throws TransloaditRequestException, TransloaditSignatureException {
        return get(url, new HashMap<>());
    }

    HttpResponse<JsonNode> post(String url, Map<String, Object> data)
            throws TransloaditRequestException, TransloaditSignatureException {
        return post(url, data, new HashMap<>());
    }

    HttpResponse<JsonNode> post(String url, Map<String, Object> data, Map<String, Object> extraData)
            throws TransloaditRequestException, TransloaditSignatureException {
        Map<String, Object> payload = toPayload(data);
        payload.putAll(extraData);

        try {
            return Unirest.post(getFullUrl(url))
                    .fields(payload)
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    HttpResponse<JsonNode> delete(String url, Map<String, Object> data)
            throws TransloaditRequestException, TransloaditSignatureException {
        try {
            return Unirest.delete(getFullUrl(url))
                    .fields(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    HttpResponse<JsonNode> put(String url, Map<String, Object> data)
            throws TransloaditRequestException, TransloaditSignatureException {
        try {
            return Unirest.put(getFullUrl(url))
                    .fields(toPayload(data))
                    .asJson();
        } catch (UnirestException e) {
            throw new TransloaditRequestException(e);
        }
    }

    private String getFullUrl(String url) {
        return url.startsWith("https://") || url.startsWith("http://") ? url : transloadit.hostUrl + url;
    }

    private Map<String, Object> toPayload(Map<String, Object> data) throws TransloaditSignatureException {
        Map<String, Object> dataClone = new HashMap<>(data);
        dataClone.put("auth", getAuthData());

        Map<String, Object> payload = new HashMap<>();
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
        Map<String, String> authData = new HashMap<>();
        authData.put("key", transloadit.key);

        Instant expiryTime = Instant.now().plusSeconds(transloadit.duration);
        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("Y/MM/dd HH:mm:ss+00:00")
                .withZone(ZoneOffset.UTC);

        authData.put("expires", formatter.format(expiryTime));

        return authData;
    }

    /**
     *
     * @param message String data that needs to be encrypted.
     * @return signature generate based on the message passed and the transloadit secret.
     */
    private String getSignature(String message) throws TransloaditSignatureException {
        byte[] kSecret = transloadit.secret.getBytes(StandardCharsets.UTF_8);
        byte[] rawHmac = HmacSHA1(kSecret, message);
        byte[] hexBytes = new Hex().encode(rawHmac);

        return new String(hexBytes, StandardCharsets.UTF_8);
    }

    private byte[] HmacSHA1(byte[] key, String data) throws TransloaditSignatureException {
        final String ALGORITHM = "HmacSHA1";
        Mac mac;

        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new TransloaditSignatureException(e);
        }

        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}

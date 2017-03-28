package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.apache.commons.codec.binary.Hex;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    private OkHttpClient httpClient = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    Request(Transloadit transloadit) {
        this.transloadit = transloadit;
    }

    okhttp3.Response get(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {

        String fullUrl = getFullUrl(url);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(buildUlr(fullUrl, toPayload(data)))
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    okhttp3.Response get(String url) throws RequestException, LocalOperationException {
        return get(url, new HashMap<String, Object>());
    }

    okhttp3.Response post(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {
        return post(url, data, new HashMap<String, Object>());
    }

    okhttp3.Response post(String url, Map<String, Object> data, Map<String, Object> extraData)
            throws RequestException, LocalOperationException {
        Map<String, Object> payload = toPayload(data);
        payload.putAll(extraData);

        RequestBody body = RequestBody.create(JSON, jsonifyData(payload));

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getFullUrl(url))
                .post(body)
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    okhttp3.Response delete(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {
        RequestBody body = RequestBody.create(JSON, jsonifyData(toPayload(data)));

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getFullUrl(url))
                .delete(body)
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    okhttp3.Response put(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {
        RequestBody body = RequestBody.create(JSON, jsonifyData(toPayload(data)));

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getFullUrl(url))
                .put(body)
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    private String getFullUrl(String url) {
        return url.startsWith("https://") || url.startsWith("http://") ? url : transloadit.getHostUrl() + url;
    }

    private String buildUlr(String url, Map<String, Object> params) throws LocalOperationException {
        StringBuilder sb = new StringBuilder();
        for(HashMap.Entry<String, Object> entry : params.entrySet()){
            if(sb.length() > 0){
                sb.append('&');
            }

            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8")).append('=')
                        .append(URLEncoder.encode((String) entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new LocalOperationException(e);
            }
        }

        return url + "?" + sb.toString();
    }

    private Map<String, Object> toPayload(Map<String, Object> data) throws LocalOperationException {
        Map<String, Object> dataClone = new HashMap<String, Object>(data);
        dataClone.put("auth", getAuthData());

        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("params", jsonifyData(dataClone));

        if (transloadit.shouldSignRequest) {
            payload.put("signature", getSignature(jsonifyData(dataClone)));
        }
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

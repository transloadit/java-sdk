package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.Nullable;
import org.joda.time.Instant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONObject;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Transloadit tailored Http Request class
 */
public class Request {
    private Transloadit transloadit;
    private OkHttpClient httpClient = new OkHttpClient();
    private String version;

    Request(Transloadit transloadit) {
        this.transloadit = transloadit;
        Properties prop = new Properties();
        InputStream in = getClass().getClassLoader().getResourceAsStream("version.properties");
        try {
            prop.load(in);
            version = "java-sdk:" + prop.getProperty("versionNumber").replace("'", "");
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException npe) {
            version = "unknown";
        }


    }

    /**
     * Makes http GET request.
     * @param url url to makes request to
     * @param params data to add to params field
     * @return {@link okhttp3.Response}
     * @throws RequestException
     * @throws LocalOperationException
     */
    okhttp3.Response get(String url, Map<String, Object> params)
            throws RequestException, LocalOperationException {

        String fullUrl = getFullUrl(url);
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(addUrlParams(fullUrl, toPayload(params)))
                .addHeader("Transloadit-Client", version)
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

    /**
     * Makes http POST request
     * @param url url to makes request to
     * @param params data to add to params field
     * @param extraData data to send along with request body, outside of params field.
     * @param files files to be uploaded along with the request.
     * @return {@link okhttp3.Response}
     * @throws RequestException
     * @throws LocalOperationException
     */
    okhttp3.Response post(String url, Map<String, Object> params,
                          @Nullable Map<String, String> extraData,
                          @Nullable Map<String, File> files, @Nullable Map<String, InputStream> fileStreams)
            throws RequestException, LocalOperationException {

        Map<String, String> payload = toPayload(params);
        if (extraData != null) {
            payload.putAll(extraData);
        }

        okhttp3.Request request = new okhttp3.Request.Builder().url(getFullUrl(url))
                .post(getBody(payload, files, fileStreams))
                .addHeader("Transloadit-Client", version)
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    okhttp3.Response post(String url, Map<String, Object> params)
            throws RequestException, LocalOperationException {
        return post(url, params, null, null, null);
    }

    /**
     * Makes http DELETE request
     * @param url url to makes request to
     * @param params data to add to params field
     * @return {@link okhttp3.Response}
     * @throws RequestException
     * @throws LocalOperationException
     */
    okhttp3.Response delete(String url, Map<String, Object> params)
            throws RequestException, LocalOperationException {
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getFullUrl(url))
                .delete(getBody(toPayload(params), null))
                .addHeader("Transloadit-Client", version)
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    /**
     * Makes http PUT request
     * @param url
     * @param data
     * @return
     * @throws RequestException
     * @throws LocalOperationException
     */
    okhttp3.Response put(String url, Map<String, Object> data)
            throws RequestException, LocalOperationException {

        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(getFullUrl(url))
                .put(getBody(toPayload(data), null))
                .addHeader("Transloadit-Client", version)
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    /**
     * Converts url path to the Transloadit full url.
     * Returns the url passed if it is already full.
     *
     * @param url
     * @return String
     */
    private String getFullUrl(String url) {
        return url.startsWith("https://") || url.startsWith("http://") ? url : transloadit.getHostUrl() + url;
    }

    private String addUrlParams(String url, Map<String, ? extends Object> params) throws LocalOperationException {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, ? extends Object> entry : params.entrySet()){
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

    /**
     * Builds okhttp3 compatible request body with the data passed.
     *
     * @param data data to add to request body
     * @param files files to upload
     * @return {@link RequestBody}
     */
    private RequestBody getBody(Map<String, String> data, @Nullable Map<String, File> files, @Nullable Map<String,
            InputStream> fileStreams) throws LocalOperationException {
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        if (files != null) {
            for (Map.Entry<String, File> entry : files.entrySet()) {
                File file = entry.getValue();
                String mimeType = URLConnection.guessContentTypeFromName(file.getName());

                if (mimeType == null) {
                    mimeType = "application/octet-stream";
                }

                builder.addFormDataPart(entry.getKey(), file.getName(),
                        RequestBody.create(MediaType.parse(mimeType), file));
            }
        }

        if (fileStreams != null) {
            for (Map.Entry<String, InputStream> entry : fileStreams.entrySet()) {
                byte[] bytes;
                InputStream stream = entry.getValue();
                try {
                    bytes = new byte[stream.available()];
                    stream.read(bytes);

                } catch (IOException e) {
                    throw new LocalOperationException(e);
                }
                builder.addFormDataPart(entry.getKey(), null,
                        RequestBody.create(MediaType.parse("application/octet-stream"), bytes));
            }
        }

        for (Map.Entry<String, String> entry : data.entrySet()) {
            builder.addFormDataPart(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    private RequestBody getBody(Map<String, String> data, @Nullable Map<String, File> files) throws LocalOperationException {
        return getBody(data, files, null);
    }

    /**
     * Returns data tree structured as Transloadit expects it.
     *
     * @param data
     * @return {@link Map}
     * @throws LocalOperationException
     */
    private Map<String, String> toPayload(Map<String, Object> data) throws LocalOperationException {
        Map<String, Object> dataClone = new HashMap<String, Object>(data);
        dataClone.put("auth", getAuthData());

        Map<String, String> payload = new HashMap<String, String>();
        payload.put("params", jsonifyData(dataClone));

        if (transloadit.shouldSignRequest) {
            payload.put("signature", getSignature(jsonifyData(dataClone)));
        }
        return payload;
    }

    /**
     * converts Map of data to json string
     *
     * @param data map data to converted to json
     * @return {@link String}
     */
    private String jsonifyData(Map<String, ? extends Object> data) {
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

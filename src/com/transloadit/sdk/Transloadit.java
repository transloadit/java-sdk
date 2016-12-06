package com.transloadit.sdk;

import org.apache.commons.codec.binary.Hex;

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
 * This class serves as a client interface to the Transloadit API
 */
public class Transloadit {
    private String key;
    private String secret;
    private String expires;

    /**
     * A new instance to transloadit client
     *
     * @param key User's transloadit key
     * @param secret User's transloadit secret
     */
    public Transloadit(String key, String secret, long duration) {
        this.key = key;
        this.secret = secret;

        Instant expiryTime = Instant.now().plusSeconds(duration);

        DateTimeFormatter formatter = DateTimeFormatter
                .ofPattern("Y/M/dd HH:mm:ss+00:00")
                .withZone(ZoneOffset.UTC);

        expires = formatter.format(expiryTime);
    }

    /**
     *
     * @return an assembly instance ({@link Assembly ) tied with the transloadit client.
     */
    public Assembly assembly() {
        return new Assembly(this);
    }

    /**
     *
     * @return Map containing authentication key and the time it expires
     */
    public Map<String, String> getAuthData() {
        Map<String, String> authData = new HashMap<>();
        authData.put("key", key);
        authData.put("expires", expires);

        return authData;
    }

    /**
     *
     * @param message String data that needs to be encrypted.
     * @return signature generate based on the message passed and the transloadit secret.
     */
    public String getSignature(String message) {
        byte[] kSecret = secret.getBytes(StandardCharsets.UTF_8);
        byte[] rawHmac = HmacSHA1(kSecret, message);
        byte[] hexBytes = new Hex().encode(rawHmac);

        return new String(hexBytes, StandardCharsets.UTF_8);
    }

    private byte[] HmacSHA1(byte[] key, String data) {
        final String ALGORITHM = "HmacSHA1";
        Mac mac = null;

        try {
            mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

}

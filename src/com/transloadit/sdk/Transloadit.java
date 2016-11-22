package com.transloadit.sdk;

import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


public class Transloadit {
    public String key;
    public String secret;

    public Transloadit(String key, String secret) {
        this.key = key;
        this.secret = secret;
    }

    public Assembly assembly() {
        return new Assembly(this);
    }

    private byte[] HmacSHA1(byte[] key, String data) {
        String algorithm = "HmacSHA1";
        Mac mac = null;
        try {
            mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    public String getSignatureKey(String message) {
        byte[] kSecret = this.secret.getBytes(StandardCharsets.UTF_8);
        byte[] rawHmac = HmacSHA1(kSecret, message);
        byte[] hexBytes = new Hex().encode(rawHmac);

        return new String(hexBytes, StandardCharsets.UTF_8);
    }
}

package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.apache.commons.codec.binary.Hex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This class serves as a client interface to the Transloadit API.
 */
public class Transloadit {
    /**
     * Default url of the Transloadit API.
     */
    public static final String DEFAULT_HOST_URL = "https://api2.transloadit.com";
    String key;
    String secret;
    long duration;
    private String hostUrl;
    boolean shouldSignRequest;
    protected int retryAttemptsRateLimit = 3;  // default value
    protected int retryAttemptsRequestException = 0; // default value
    protected ArrayList<String> qualifiedErrorsForRetry;
    protected int retryDelay = 0; // default value
    protected String versionInfo;
    private SignatureProvider signatureProvider;

    /**
     * A new instance to transloadit client.
     *
     * @param key User's transloadit key
     * @param secret User's transloadit secret.
     * @param duration for how long (in seconds) the request should be valid.
     * @param hostUrl the host url to the transloadit service.
     */
    public Transloadit(String key, @Nullable String secret, long duration, String hostUrl) {
        this.key = key;
        this.secret = secret;
        this.duration = duration;
        this.hostUrl = hostUrl;
        this.shouldSignRequest = secret != null;
        this.qualifiedErrorsForRetry = new ArrayList<String>(
                Collections.singletonList("java.net.SocketTimeoutException"));
        this.versionInfo = loadVersionInfo();
    }
    /**
     * A new instance to transloadit client.
     *
     * @param key User's transloadit key
     * @param secret User's transloadit secret.
     * @param duration for how long (in seconds) the request should be valid.
     */
    public Transloadit(String key, String secret, long duration) {
        this(key, secret, duration, DEFAULT_HOST_URL);
    }

    /**
     * A new instance to transloadit client.
     *
     * @param key User's transloadit key
     * @param secret User's transloadit secret.
     * @param hostUrl the host url to the transloadit service.
     */
    public Transloadit(String key, String secret, String hostUrl) {
        this(key, secret, 5 * 60, hostUrl);
    }

    /**
     * A new instance to transloadit client.
     *
     * @param key User's transloadit key
     * @param secret User's transloadit secret.
     */
    public Transloadit(String key, String secret) {
        this(key, secret, 5 * 60, DEFAULT_HOST_URL);
    }

    /**
     * A new instance to transloadit client with external signature generation.
     *
     * @param key User's transloadit key
     * @param signatureProvider Provider for generating signatures externally
     * @param duration for how long (in seconds) the request should be valid.
     * @param hostUrl the host url to the transloadit service.
     * @since 2.1.0
     */
    public Transloadit(String key, SignatureProvider signatureProvider, long duration, String hostUrl) {
        this(key, (String) null, duration, hostUrl);  // Explicit cast to avoid ambiguity
        setSignatureProvider(Objects.requireNonNull(signatureProvider, "signatureProvider must not be null"));
    }

    /**
     * A new instance to transloadit client with external signature generation.
     *
     * @param key User's transloadit key
     * @param signatureProvider Provider for generating signatures externally
     * @param duration for how long (in seconds) the request should be valid.
     * @since 2.1.0
     */
    public Transloadit(String key, SignatureProvider signatureProvider, long duration) {
        this(key, signatureProvider, duration, DEFAULT_HOST_URL);
    }

    /**
     * A new instance to transloadit client with external signature generation.
     *
     * @param key User's transloadit key
     * @param signatureProvider Provider for generating signatures externally
     * @param hostUrl the host url to the transloadit service.
     * @since 2.1.0
     */
    public Transloadit(String key, SignatureProvider signatureProvider, String hostUrl) {
        this(key, signatureProvider, 5 * 60, hostUrl);
    }

    /**
     * A new instance to transloadit client with external signature generation.
     *
     * @param key User's transloadit key
     * @param signatureProvider Provider for generating signatures externally
     * @since 2.1.0
     */
    public Transloadit(String key, SignatureProvider signatureProvider) {
        this(key, signatureProvider, 5 * 60, DEFAULT_HOST_URL);
    }

    /**
     * Enable/Disable request signing.
     * @param flag the boolean value to set it to.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public void setRequestSigning(boolean flag) throws LocalOperationException {
        if (flag && secret == null && signatureProvider == null) {
            throw new LocalOperationException("Cannot enable request signing with null secret and no signature provider.");
        } else {
            shouldSignRequest = flag;
        }
    }

    /**
     * Gets the signature provider if one has been set.
     *
     * @return The signature provider, or null if using built-in signature generation
     * @since 2.1.0
     */
    public SignatureProvider getSignatureProvider() {
        return signatureProvider;
    }

    /**
     * Sets a signature provider for external signature generation.
     *
     * <p>When a signature provider is set, it will be used instead of the built-in
     * signature generation. This allows you to generate signatures on your backend
     * server for improved security.</p>
     *
     * @param signatureProvider The signature provider to use, or null to use built-in generation
     *                          (disabling signing entirely when no secret is configured)
     * @since 2.1.0
     */
    public void setSignatureProvider(@Nullable SignatureProvider signatureProvider) {
        this.signatureProvider = signatureProvider;
        if (signatureProvider != null) {
            this.shouldSignRequest = true;
        } else {
            this.shouldSignRequest = this.secret != null;
        }
    }

    /**
     * Loads the current version from the version.properties File and builds an Info String for the
     * Transloadit-Client header.
     * @return String with content: java-sdk:version
     */
    protected String loadVersionInfo() {
        Properties prop = new Properties();
        String versionInfo;
        InputStream in = getClass().getClassLoader().getResourceAsStream("java-sdk-version/version.properties");
        try {
            prop.load(in);
            versionInfo = "java-sdk:" + prop.getProperty("versionNumber").replace("'", "");
            in.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException npe) {
            versionInfo = "java-sdk:unknown";
        }
        return versionInfo;
    }

    /**
     * Returns a version Info String, which can be sent as header-value with a {@link Request}.
     * @return Version info as Header-String
     */
    String getVersionInfo() {
        return this.versionInfo;
    }


    /**
     * Adjusts number of retry attempts that should be taken if a "RATE_LIMIT_REACHED" error appears
     * during assembly processing.
     * Default value for every Transloadit instance is 3 retries.
     * @param retryAttemptsRateLimit number of retry attempts
     * @throws LocalOperationException if provided number is negative
     */
    public void setRetryAttemptsRateLimit(int retryAttemptsRateLimit) throws LocalOperationException {
        if (retryAttemptsRateLimit < 0) {
            throw new LocalOperationException("No negative number of retry Attempts possible.");
        } else {
            this.retryAttemptsRateLimit = retryAttemptsRateLimit;
        }
    }

    /**
     * Returns number of retry attempts that should be taken in case of a "RATE_LIMIT_REACHED" error appears
     * during assembly processing.
     * @return number of retry attempts
     */
    public int getRetryAttemptsRateLimit() {
        return retryAttemptsRateLimit;
    }


    /**
     *  This is an experimental debugging feature and should therefore best not be turned on at all or only with
     *  the utmost caution.
     * This method adjusts number of retry attempts that should be taken if specific "REQUEST_EXCEPTION" are
     * occuring during assembly processing.
     * The Default value for every Transloadit instance is 0 extra retries.
     * All retry attempts are made in addition to those made by the HTTP library. As a result the effective number
     * of retries can be higher. Also this value should be handled with care as fast failing could be a wanted
     * behaviour.
     * @param retryAttemptsRequestException Number of extra retry attempts
     */
    public void setRetryAttemptsRequestException(int retryAttemptsRequestException) {
        if (retryAttemptsRequestException > 0) {
            this.retryAttemptsRequestException = retryAttemptsRequestException;
        } else {
            this.retryAttemptsRequestException = 0;
        }
    }

    /**
     * Returns number of retry attempts that should be taken in case of a "REQUEST_EXCEPTION" appears caused by timeout
     * during assembly processing.
     * @return number of retry attempts
     */
    public int getRetryAttemptsRequestException() {
        return retryAttemptsRequestException;
    }

    /**
     *
     * @return the host url of the Transloadit server.
     */
    public String getHostUrl() {
        return hostUrl;
    }

    /**
     * Returns an Assembly instance that can be used to create a new assembly.
     *
     * @return {@link Assembly}
     */
    public Assembly newAssembly() {
        return new Assembly(this);
    }

    /**
     * Returns a single assembly.
     *
     * @param id id of the Assembly to retrieve.
     * @return {@link AssemblyResponse}
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public AssemblyResponse getAssembly(String id) throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new AssemblyResponse(request.get("/assemblies/" + id));
    }

    /**
     * Returns a single assembly.
     *
     * @param url full url of the Assembly.
     * @return {@link AssemblyResponse}
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public AssemblyResponse getAssemblyByUrl(String url)
            throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new AssemblyResponse(request.get(url));
    }

    /**
     * cancels a running assembly.
     *
     * @param url full url of the Assembly.
     * @return {@link AssemblyResponse}
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public AssemblyResponse cancelAssembly(String url)
            throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new AssemblyResponse(request.delete(url, new HashMap<String, Object>()));
    }

    /**
     * Returns a list of all assemblies under the user account.
     *
     * @param options {@link Map} extra options to send along with the request.
     * @return {@link ListResponse}
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public ListResponse listAssemblies(Map<String, Object> options)
            throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new ListResponse(request.get("/assemblies", options));
    }

    /**
     * Returns a list of all assemblies under the user account.
     * @return {@link ListResponse}
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public ListResponse listAssemblies() throws RequestException, LocalOperationException {
        return listAssemblies(new HashMap<String, Object>());
    }

    /**
     * Returns a Template instance that can be used to create a new template.
     * @param name name of the template.
     *
     * @return {@link Template}
     */
    public Template newTemplate(String name) {
        return new Template(this, name);
    }

    /**
     * Returns a single template.
     *
     * @param id id of the template to retrieve.
     * @return {@link Response}
     *
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public Response getTemplate(String id) throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new Response(request.get("/templates/" + id));
    }

    /**
     * Updates the template with the specified id.
     *
     * @param id id of the template to update
     * @param options a Map of options to update/add.
     * @return {@link Response}
     *
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public Response updateTemplate(String id, Map<String, Object> options)
            throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new Response(request.put("/templates/" + id, options));
    }

    /**
     * Deletes a template.
     *
     * @param id id of the template to delete.
     * @return {@link Response}
     *
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public Response deleteTemplate(String id)
            throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new Response(request.delete("/templates/" + id, new HashMap<String, Object>()));
    }

    /**
     * Returns a list of all templates under the user account.
     *
     * @param options {@link Map} extra options to send along with the request.
     * @return {@link ListResponse}
     *
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public ListResponse listTemplates(Map<String, Object> options)
            throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new ListResponse(request.get("/templates", options));
    }

    /**
     * Returns a list of all templates under the user account.
     *
     * @return {@link ListResponse}
     *
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public ListResponse listTemplates()
            throws RequestException, LocalOperationException {
        return listTemplates(new HashMap<String, Object>());
    }

    /**
     * Returns the bill for the month specified.
     *
     * @param month for which bill to retrieve.
     * @param year for which bill to retrieve.
     * @return {@link Response}
     *
     * @throws RequestException if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public Response getBill(int month, int year)
            throws RequestException, LocalOperationException {
        Request request = new Request(this);
        return new Response(request.get("/bill/" + year + String.format("-%02d", month)));
    }

    /**
     * Returns Array List of String encoded Exceptions, which should be qualified for a retry attempt.
     * {@code "java.net.SocketTimeoutException" } is added by default
     * @return Array List of String encoded Exceptions
     */
    public ArrayList<String> getQualifiedErrorsForRetry() {
        return qualifiedErrorsForRetry;
    }

    /**
     * Array List of String encoded Exceptions, which should be qualified for a retry attempt.
     * !! This is a debugging feature, do not use by default.
     * {@code "java.net.SocketTimeoutException" } is added by default, but can be overwritten.
     * @param qualifiedErrorsForRetry String encoded Exception e.g. "java.net.SocketTimeoutException"
     */
    public void setQualifiedErrorsForRetry(ArrayList<String> qualifiedErrorsForRetry) {
        this.qualifiedErrorsForRetry = qualifiedErrorsForRetry;
    }

    /**
     * Returns the retry delay in milliseconds, which is applied in addition to a random component of 0 - 1000 ms in
     * cases of request retry.
     * @return Timeout in ms
     */
    public int getRetryDelay() {
        return retryDelay;
    }

    /**
     * Sets the retry delay in milliseconds, which is applied in addition to a random component of 0 - 1000 ms in cases
     * of request retry.
     * @param delay in ms
     * @throws LocalOperationException if provided timeout is smaller than 0
     */
    public void setRetryDelay(int delay) throws LocalOperationException {
        if (delay < 0) {
            throw new LocalOperationException("Timeout invalid. Values > 0 are expected");
        } else {
            this.retryDelay = delay;
        }
    }

    /**
     * Construct a signed Smart CDN URL.
     * See the <a href="https://transloadit.com/docs/topics/signature-authentication/#smart-cdn">API documentation</a>.
     * Same as {@link Transloadit#getSignedSmartCDNUrl(String, String, String, Map, long)}, but with an expiration in 1 hour.
     *
     * @param workspace Workspace slug
     * @param template Template slug or template ID
     * @param input Input value that is provided as ${fields.input} in the template
     * @param urlParams Additional parameters for the URL query string (optional)
     * @return The signed Smart CDN URL
     */
    public String getSignedSmartCDNUrl(@NotNull String workspace, @NotNull String template, @NotNull String input,
                                       @Nullable Map<String, List<String>> urlParams) throws LocalOperationException {
        // 1 hours default expiration
        long expiresAt = Instant.now().toEpochMilli() + 60 * 60 * 1000;
        return getSignedSmartCDNUrl(workspace, template, input, urlParams, expiresAt);
    }

    /**
     * Construct a signed Smart CDN URL.
     * See the <a href="https://transloadit.com/docs/topics/signature-authentication/#smart-cdn">API documentation</a>.
     *
     * @param workspace Workspace slug
     * @param template Template slug or template ID
     * @param input Input value that is provided as ${fields.input} in the template
     * @param urlParams Additional parameters for the URL query string (optional)
     * @param expiresAt Expiration timestamp of the signature in milliseconds since the UNIX epoch.
     * @return The signed Smart CDN URL
     */
    public String getSignedSmartCDNUrl(@NotNull String workspace, @NotNull String template, @NotNull String input,
                                       @Nullable Map<String, List<String>> urlParams, long expiresAt) throws LocalOperationException {

        try {
            String workspaceSlug = URLEncoder.encode(workspace, StandardCharsets.UTF_8.name());
            String templateSlug = URLEncoder.encode(template, StandardCharsets.UTF_8.name());
            String inputField = URLEncoder.encode(input, StandardCharsets.UTF_8.name());

            // Use TreeMap to ensure keys in URL params are sorted.
            SortedMap<String, List<String>> params = new TreeMap<>(urlParams);
            params.put("auth_key", Collections.singletonList(this.key));
            params.put("exp", Collections.singletonList(String.valueOf(expiresAt)));

            List<String> queryParts = new ArrayList<>(params.size());
            for (Map.Entry<String, List<String>> entry : params.entrySet()) {
                String key = entry.getKey();
                for (String value : entry.getValue()) {
                    queryParts.add(URLEncoder.encode(key, StandardCharsets.UTF_8.name()) + "=" + URLEncoder.encode(
                            value, StandardCharsets.UTF_8.name()));
                }
            }

            String queryString = String.join("&", queryParts);
            String stringToSign = workspaceSlug + "/" + templateSlug + "/" + inputField + "?" + queryString;

            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                this.secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKey);
            byte[] signatureBytes = hmac.doFinal(stringToSign.getBytes());
            byte[] signatureHexBytes = new Hex().encode((signatureBytes));
            String signature = "sha256:" + new String(signatureHexBytes, StandardCharsets.UTF_8);
            String signatureEncoded = URLEncoder.encode(signature, StandardCharsets.UTF_8.name());

            return "https://" + workspaceSlug + ".tlcdn.com/" + templateSlug + "/"
                    + inputField + "?" + queryString + "&sig=" + signatureEncoded;
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new LocalOperationException("Failed to create signature: " + e.getMessage());
        }
    }
}

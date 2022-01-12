package com.transloadit.sdk;

import com.transloadit.sdk.async.AssemblyProgressListener;
import com.transloadit.sdk.async.UploadProgressListener;
import com.transloadit.sdk.async.AsyncAssembly;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
    protected ArrayList<String> additionalTransloaditClientHeaderContent;

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
        this.qualifiedErrorsForRetry = new ArrayList<String>(Collections.singletonList("java.net.SocketTimeoutException"));
        this.additionalTransloaditClientHeaderContent = new ArrayList<String>();
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
     * Enable/Disable request signing.
     * @param flag the boolean value to set it to.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public void setRequestSigning(boolean flag) throws LocalOperationException {
        if (flag && secret == null) {
            throw new LocalOperationException("Cannot enable request signing with null secret.");
        } else {
            shouldSignRequest = flag;
        }
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
     *  <h1>This is an experimental debugging feature and should therefore best not be turned on at all or only with
     *  the utmost caution.</h1>
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
     * Returns an AsyncAssembly instance that can be used to create a new assembly asynchronously.
     * This method is good for running assemblies in the background
     *
     * @param listener an implementation of {@link UploadProgressListener} to serve as a callback
     *                 for the asynchronous assembly.
     * @return {@link AsyncAssembly}
     */
    public AsyncAssembly newAssembly(UploadProgressListener listener) {
        return new AsyncAssembly(this, listener);
    }

    /**
     * Returns an AsyncAssembly instance that can be used to create a new assembly asynchronously.
     * This method is good for running assemblies in the background
     *
     * @param listener an implementation of {@link AssemblyProgressListener} to serve as a callback
     *                 for the asynchronous assembly.
     * @deprecated use {@link #newAssembly(UploadProgressListener)} instead
     * @return {@link AsyncAssembly}
     */
    public AsyncAssembly newAssembly(AssemblyProgressListener listener) {
        return new AsyncAssembly(this, listener);
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
     * Returns additional Information which will be sent alongside with the request in the Transloadit-Client Header.
     * @return List of additional Transloadit Headers
     */
    public ArrayList getAdditionalTransloaditClientHeaderContent() {
        return this.additionalTransloaditClientHeaderContent;
    }

    /**
     * Adds Information, which will be sent alongside with the request in the Transloadit-Client Header.
     * @param sdkName Name of the used extra Software / SDK
     * @param versionNumber Semantic Version Number of the used SDK
     * @throws LocalOperationException if version number has a wrong input format or the sdkName contains illegal characters
     */
    public void setAdditionalTransloaditClientHeaderContent(String sdkName, String versionNumber) throws LocalOperationException {
        versionNumber = versionNumber.replaceAll("\\s+", "");
        sdkName = sdkName.replaceAll("\\s+", "");
        Pattern illegalChars = Pattern.compile("[.:,;\"'\\+]", Pattern.CASE_INSENSITIVE);
        Pattern semanticVersion = Pattern.compile("^([0-9]+)\\.([0-9]+)\\.([0-9]+)", Pattern.CASE_INSENSITIVE);

        Matcher charMatcher = illegalChars.matcher(sdkName);
        Matcher versionMatcher = semanticVersion.matcher(versionNumber);
        if (charMatcher.find() || !versionMatcher.matches()) {
            throw new LocalOperationException("Provided version number does not match expected format of"
                   + "  ^([0-9]+)\\.([0-9]+)\\.([0-9]+)"  + " or sdkName contains  [.:,;\"'\\+]");
        }
        String header = sdkName + ":" + versionNumber;
        additionalTransloaditClientHeaderContent.add(header);
    }
}

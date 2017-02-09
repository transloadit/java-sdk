package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;
import com.transloadit.sdk.response.Response;

import java.util.HashMap;
import java.util.Map;


/**
 * This class serves as a client interface to the Transloadit API
 */
public class Transloadit {
    public static final String DEFAULT_HOST_URL = "https://api2.transloadit.com";
    String key;
    String secret;
    long duration;
    String hostUrl;

    /**
     * A new instance to transloadit client
     *
     * @param key User's transloadit key
     * @param secret User's transloadit secret.
     * @param duration for how long (in seconds) the request should be valid.
     */
    public Transloadit(String key, String secret, long duration, String hostUrl) {
        this.key = key;
        this.secret = secret;
        this.duration = duration;
        this.hostUrl = hostUrl;
    }

    public Transloadit(String key, String secret, long duration) {
        this(key, secret, duration, DEFAULT_HOST_URL);
    }

    public Transloadit(String key, String secret, String hostUrl) {
        this(key, secret, 5 * 60, hostUrl);
    }

    public Transloadit(String key, String secret) {
        this(key, secret, 5 * 60, DEFAULT_HOST_URL);
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
     * @param id id of the Assebly to retrieve.
     * @return {@link AssemblyResponse}
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public AssemblyResponse getAssembly(String id) throws TransloaditRequestException, TransloaditSignatureException {
        Request request = new Request(this);
        return new AssemblyResponse(request.get("/assemblies/" + id));
    }

    /**
     * Returns a single assembly.
     *
     * @param url full url of the Assembly.
     * @return {@link AssemblyResponse}
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public AssemblyResponse getAssemblyByUrl(String url)
            throws TransloaditRequestException, TransloaditSignatureException {
        Request request = new Request(this);
        return new AssemblyResponse(request.get(url));
    }

    /**
     * Returns a list of all assemblies under the user account
     *
     * @param options {@link Map} extra options to send along with the request.
     * @return {@link ListResponse}
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public ListResponse listAssemblies(Map<String, Object> options)
            throws TransloaditRequestException, TransloaditSignatureException {
        Request request = new Request(this);
        return new ListResponse(request.get("/assemblies", options));
    }

    public ListResponse listAssemblies() throws TransloaditRequestException, TransloaditSignatureException {
        return listAssemblies(new HashMap<>());
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
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public Response getTemplate(String id) throws TransloaditRequestException, TransloaditSignatureException {
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
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public Response updateTemplate(String id, Map<String, Object> options)
            throws TransloaditRequestException, TransloaditSignatureException {
        Request request = new Request(this);
        return new Response(request.put("/templates/" + id, options));
    }

    /**
     * Deletes a template.
     *
     * @param id id of the template to delete.
     * @return {@link Response}
     *
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public Response deleteTemplate(String id) throws TransloaditRequestException, TransloaditSignatureException {
        Request request = new Request(this);
        return new Response(request.delete("/templates/" + id, new HashMap<>()));
    }

    /**
     * Returns a list of all templates under the user account
     *
     * @param options {@link Map} extra options to send along with the request.
     * @return {@link ListResponse}
     *
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public ListResponse listTemplates(Map<String, Object> options)
            throws TransloaditRequestException, TransloaditSignatureException {
        Request request = new Request(this);
        return new ListResponse(request.get("/templates", options));
    }

    public ListResponse listTemplates()
            throws TransloaditRequestException, TransloaditSignatureException {
        return listTemplates(new HashMap<>());
    }

    /**
     * Returns the bill for the month specified.
     *
     * @param month for which bill to retrieve.
     * @param year for which bill to retrieve.
     * @return {@link Response}
     *
     * @throws TransloaditRequestException
     * @throws TransloaditSignatureException
     */
    public Response getBill(int month, int year) throws TransloaditRequestException, TransloaditSignatureException {
        Request request = new Request(this);
        return new Response(request.get("/bill/" + year + String.format("-%02d", month)));
    }
}

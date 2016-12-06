package com.transloadit.sdk;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Model for Tranloadit Assembly Api
 */
public class Assembly {
    public Transloadit transloadit;

    public Assembly(Transloadit transloadit) {
        this.transloadit = transloadit;
    }

    /**
     *
     * Creates a new assembly
     *
     * @param options
     * @param files array of files to upload.
     * @return
     */
    public AssemblyResponse create(Map<String, Object> options, File[] files) throws UnirestException {
        Request request = new Request(transloadit);
        return new AssemblyResponse(request.post("/assemblies", options, mapFiles(files)));
    }

    /**
     * Returns a list of all assemblies under the user account
     *
     * @param options
     * @return
     * @throws UnirestException
     */
    public ListResponse list(Map<String, Object> options) throws UnirestException {
        Request request = new Request(transloadit);
        return new ListResponse(request.get("/assemblies", options));
    }

    /**
     * Returns a single assembly.
     *
     * @param id id of the Assebly to retrieve.
     * @return
     * @throws UnirestException
     */
    public AssemblyResponse get(String id) throws UnirestException {
        Request request = new Request(transloadit);
        return new AssemblyResponse(request.get("/assemblies/" + id));
    }

    private Map<String, Object> mapFiles(File[] files){
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < files.length; i++) {
            map.put("file_" + i, files[i]);
        }

        return map;
    }

}

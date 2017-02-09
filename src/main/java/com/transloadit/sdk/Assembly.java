package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.TransloaditRequestException;
import com.transloadit.sdk.exceptions.TransloaditSignatureException;
import com.transloadit.sdk.response.AssemblyResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a new assembly being created
 */
public class Assembly extends OptionsBuilder {
    Map<String, Object> files;

    public Assembly(Transloadit transloadit) {
        this(transloadit, new Steps(), new HashMap<>(), new HashMap<>());
    }

    /**
     *
     * @param steps {@link Steps} the steps to add to the assembly.
     * @param files is a map of file names and files that are meant to be uploaded.
     * @param options map of extra options to be sent along with the request.
     */
    public Assembly(Transloadit transloadit, Steps steps, Map<String, File> files, Map<String, Object> options) {
        this.transloadit = transloadit;
        this.steps = steps;
        this.files = new HashMap<>(files);
        this.options = options;
    }

    /**
     * Adds a file to your assembly.
     *
     * @param file {@link File} the file to be uploaded.
     * @param name {@link String} the name you the file to be given in transloadit
     */
    public void addFile(File file, String name){
        files.put(name, file);
    }

    /**
     * Adds a file to your assembly but automatically genarates the name of the file.
     *
     * @param file {@link File} the file to be uploaded.
     */
    public void addFile(File file){
        String name = "file_";

        for (int i = files.size(); files.containsKey(name); i++) {
            name += i;
        }
        files.put(name, file);
    }

    /**
     * Submits the configured assembly to Transloadit for processing.
     *
     * @return {@link AssemblyResponse}
     * @throws TransloaditRequestException
     */
    public AssemblyResponse save() throws TransloaditRequestException, TransloaditSignatureException {
        options.put("steps", steps.toMap());
        Request request = new Request(transloadit);
        return new AssemblyResponse(request.post("/assemblies", options, files));
    }
}

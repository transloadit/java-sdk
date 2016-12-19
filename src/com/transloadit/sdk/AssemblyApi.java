package com.transloadit.sdk;

import com.transloadit.sdk.response.AssemblyResponse;
import com.transloadit.sdk.response.ListResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Model for Tranloadit AssemblyApi Api
 */
public class AssemblyApi {
    public Transloadit transloadit;

    public AssemblyApi(Transloadit transloadit) {
        this.transloadit = transloadit;
    }

    /**
     * Returns a list of all assemblies under the user account
     *
     * @param options {@link Map} extra options to send along with the request.
     * @return
     * @throws TransloaditRequestException
     */
    public ListResponse list(Map<String, Object> options) throws TransloaditRequestException {
        Request request = new Request(transloadit);
        return new ListResponse(request.get("/assemblies", options));
    }

    public ListResponse list() throws TransloaditRequestException {
        return list(new HashMap<>());
    }

    /**
     * Returns a single assemblyApi.
     *
     * @param id id of the Assebly to retrieve.
     * @return {@link AssemblyResponse}
     * @throws TransloaditRequestException
     */
    public AssemblyResponse get(String id) throws TransloaditRequestException {
        Request request = new Request(transloadit);
        return new AssemblyResponse(request.get("/assemblies/" + id));
    }

    /**
     * Returns an Assembly instance that can be used to create a new assembly.
     *
     * @return {@link Assembly}
     */
    public Assembly new_(){
        return new Assembly();
    }

    /**
     * This class represents a new assembly being created
     */
    public class Assembly {
        private Steps steps;
        private Map<String, Object> files;
        private Map<String, Object> options;

        public Assembly() {
            this(new Steps(), new HashMap<>(), new HashMap<>());
        }

        /**
         *
         * @param steps {@link Steps} the steps to add to the assembly.
         * @param files is a map of file names and files that are meant to be uploaded.
         * @param options map of extra options to be sent along with the request.
         */
        public Assembly(Steps steps, Map<String, File> files, Map<String, Object> options) {
            this.steps = steps;
            this.files = new HashMap<>(files);
            this.options = options;
        }

        /**
         * Adds a step to the assembly.
         *
         * @param name {@link String} name of the step
         * @param robot {@link String} name of the robot used by the step.
         * @param options {@link Map} extra options required for the step.
         */
        public void addStep(String name, String robot, Map<String, Object> options){
            steps.addStep(name, robot, options);
        }

        /**
         * Adds extra options(e.g "template_id") to be sent along with the request.
         *
         * @param options {@link Map} set of options to add
         */
        public void addOptions(Map<String, Object> options) {
            this.options.putAll(options);
        }

        /**
         * Adds a single option to be sent along with the request.
         *
         * @param key {@link String} name of the option
         * @param value {@link Object} value of the option.
         */
        public void addOption(String key, Object value) {
            this.options.put(key, value);
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
        public AssemblyResponse save() throws TransloaditRequestException{
            options.put("steps", steps.toMap());
            Request request = new Request(transloadit);
            return new AssemblyResponse(request.post("/assemblies", options, files));
        }
    }

}

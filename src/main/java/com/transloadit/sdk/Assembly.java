package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import io.tus.java.client.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a new assembly being created
 */
public class Assembly extends OptionsBuilder {
    private TusURLStore tusURLStore;

    protected Map<String, File> files;
    protected Map<String, InputStream> fileStreams;
    protected TusClient tusClient;
    protected List<TusUpload> uploads;

    public Assembly(Transloadit transloadit) {
        this(transloadit, new Steps(), new HashMap<String, File>(), new HashMap<String, Object>());
    }

    /**
     * @param transloadit {@link Transloadit} the transloadit client.
     * @param steps       {@link Steps} the steps to add to the assembly.
     * @param files       is a map of file names and files that are meant to be uploaded.
     * @param options     map of extra options to be sent along with the request.
     */
    public Assembly(Transloadit transloadit, Steps steps, Map<String, File> files, Map<String, Object> options) {
        this.transloadit = transloadit;
        this.steps = steps;
        this.files = files;
        this.options = options;
        tusClient = new TusClient();
        tusURLStore = new TusURLMemoryStore();
        uploads = new ArrayList<TusUpload>();
        fileStreams = new HashMap<String, InputStream>();
    }

    /**
     * Adds a file to your assembly.
     *
     * @param file {@link File} the file to be uploaded.
     * @param name {@link String} the name you the file to be given in transloadit
     */
    public void addFile(File file, String name) {
        files.put(name, file);

        // remove duplicate key
        if (fileStreams.containsKey(name)) {
            fileStreams.remove(name);
        }
    }

    /**
     * Adds a file to your assembly but automatically genarates the name of the file.
     *
     * @param file {@link File} the file to be uploaded.
     */
    public void addFile(File file) {
        String name = "file";
        files.put(normalizeDuplicateName(name), file);
    }

    /**
     * Adds a file to your assembly but automatically genarates the name of the file.
     *
     * @param fileStream {@link InputStream} the file to be uploaded.
     */
    public void addFile(InputStream fileStream) {
        String name = "file";
        fileStreams.put(normalizeDuplicateName(name), fileStream);
    }

    /**
     * Adds a file to your assembly.
     *
     * @param inputStream {@link InputStream} the file to be uploaded.
     * @param name {@link String} the name you the file to be given in transloadit
     */
    public void addFile(InputStream inputStream, String name) {
        fileStreams.put(normalizeDuplicateName(name), inputStream);

        // remove duplicate key
        if (files.containsKey(name)) {
            files.remove(name);
        }
    }

    /**
     * Removes file from your assembly.
     *
     * @param name name of the file to remove.
     */
    public void removeFile(String name) {
        if(files.containsKey(name)) {
            files.remove(name);
        }

        if(fileStreams.containsKey(name)) {
            fileStreams.remove(name);
        }
    }

    private String normalizeDuplicateName(String name) {
        for (int i = files.size(); files.containsKey(name); i++) {
            name += "_" + i;
        }

        for (int i = fileStreams.size(); fileStreams.containsKey(name); i++) {
            name += "_" + i;
        }

        return name;
    }

    /**
     * Returns the number of files that have been added for upload
     *
     * @return the number of files that have been added for upload
     */
    public int getFilesCount() {
        return files.size() + fileStreams.size();
    }

    public void setTusURLStore(TusURLStore store) {
        tusURLStore = store;
    }

    /**
     * Submits the configured assembly to Transloadit for processing.
     *
     * @param isResumable boolean value that tells the assembly whether or not to use tus.
     * @return {@link AssemblyResponse}
     * @throws RequestException        if request to transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public AssemblyResponse save(boolean isResumable)
            throws RequestException, LocalOperationException {
        Request request = new Request(getClient());
        options.put("steps", steps.toMap());

        // only do tus uploads if files will be uploaded
        if (isResumable && getFilesCount() > 0) {
            Map<String, String> tusOptions = new HashMap<String, String>();
            tusOptions.put("tus_num_expected_upload_files", Integer.toString(getFilesCount()));

            AssemblyResponse response = new AssemblyResponse(
                    request.post("/assemblies", options, tusOptions, null, null), true);

            // check if the assembly returned an error
            if (response.hasError()) {
                throw new RequestException("Request to Assembly failed: " + response.json().getString("error"));
            }

            try {
                handleTusUpload(response);
            } catch (IOException e) {
                throw new LocalOperationException(e);
            } catch (ProtocolException e) {
                throw new RequestException(e);
            }
            return response;
        } else {
            return new AssemblyResponse(request.post("/assemblies", options, null, files, fileStreams));
        }
    }

    public AssemblyResponse save() throws LocalOperationException, RequestException {
        return this.save(true);
    }

    protected void handleTusUpload(AssemblyResponse response) throws IOException, ProtocolException {
        processTusFiles(response.getSslUrl());
        uploadTusFiles();
    }

    /**
     * @param assemblyUrl the assembly url affiliated with the tus upload
     * @throws IOException       when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    protected void processTusFiles(String assemblyUrl) throws IOException, ProtocolException {
        tusClient.setUploadCreationURL(new URL(getClient().getHostUrl() + "/resumable/files/"));
        tusClient.enableResuming(tusURLStore);

        for (Map.Entry<String, File> entry : files.entrySet()) {
            processTusFile(entry.getValue(), entry.getKey(), assemblyUrl);
        }

        for (Map.Entry<String, InputStream> entry : fileStreams.entrySet()) {
            processTusFile(entry.getValue(), entry.getKey(), assemblyUrl);
        }
    }

    protected void processTusFile(InputStream inptStream, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(inptStream, fieldName);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", fieldName);
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }


    protected void processTusFile(File file, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(file);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", file.getName());
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }

    protected TusUpload getTusUploadInstance(InputStream inputStream, String fieldName) {
        TusUpload tusUpload = new TusUpload();
        tusUpload.setInputStream(inputStream);
        tusUpload.setFingerprint(fieldName);

        return tusUpload;
    }

    protected TusUpload getTusUploadInstance(File file) throws FileNotFoundException {
        return new TusUpload(file);
    }

    void uploadTusFiles() throws IOException, ProtocolException {
        while (uploads.size() > 0) {
            final TusUploader tusUploader = tusClient.resumeOrCreateUpload(uploads.get(0));

            TusExecutor tusExecutor = new TusExecutor() {
                @Override
                protected void makeAttempt() throws ProtocolException, IOException {
                    int uploadedChunk = 0;
                    while (uploadedChunk > -1) {
                        uploadedChunk = tusUploader.uploadChunk();
                    }
                    tusUploader.finish();
                }
            };

            tusExecutor.makeAttempts();
            // remove upload instance from list
            uploads.remove(0);
        }
    }
}

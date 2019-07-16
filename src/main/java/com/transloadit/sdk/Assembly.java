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
    protected boolean shouldWaitForCompletion;

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
        shouldWaitForCompletion = false;
    }

    /**
     * Adds a file to your assembly. If the field name specified already exists, it will override the content of the
     * existing name.
     *
     * @param file {@link File} the file to be uploaded.
     * @param name {@link String} the field name of the file when submitted Transloadit.
     */
    public void addFile(File file, String name) {
        files.put(name, file);

        // remove duplicate key
        if (fileStreams.containsKey(name)) {
            fileStreams.remove(name);
        }
    }

    /**
     * Adds a file to your assembly but automatically generates the field name of the file.
     *
     * @param file {@link File} the file to be uploaded.
     */
    public void addFile(File file) {
        String name = "file";
        files.put(normalizeDuplicateName(name), file);
    }

    /**
     * Adds a file to your assembly. If the field name specified already exists, it will override the content of the
     * existing name.
     *
     * @param inputStream {@link InputStream} the file to be uploaded.
     * @param name {@link String} the field name of the file when submitted Transloadit.
     */
    public void addFile(InputStream inputStream, String name) {
        fileStreams.put(name, inputStream);

        // remove duplicate key
        if (files.containsKey(name)) {
            files.remove(name);
        }
    }

    /**
     * Adds a file to your assembly but automatically genarates the name of the file.
     *
     * @param inputStream {@link InputStream} the file to be uploaded.
     */
    public void addFile(InputStream inputStream) {
        String name = "file";
        fileStreams.put(normalizeDuplicateName(name), inputStream);
    }

    /**
     * Removes file from your assembly.
     *
     * @param name field name of the file to remove.
     */
    public void removeFile(String name) {
        if(files.containsKey(name)) {
            files.remove(name);
        }

        if(fileStreams.containsKey(name)) {
            fileStreams.remove(name);
        }
    }

    /**
     * Determine whether or not to wait till the assembly is complete after it is saved.
     *
     * @param shouldWaitForCompletion boolean value to determine whether or not to wait till the assembly is complete.
     */
    public void setShouldWaitForCompletion(boolean shouldWaitForCompletion) {
        this.shouldWaitForCompletion = shouldWaitForCompletion;
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
     * Returns the number of files that have been added for upload.
     *
     * @return the number of files that have been added for upload.
     */
    public int getFilesCount() {
        return files.size() + fileStreams.size();
    }

    /**
     * Set custom Url Storage. This should be an implementation of {@link TusURLStore}.
     *
     * @param store {@link TusURLStore} the storage instance.
     */
    public void setTusURLStore(TusURLStore store) {
        tusURLStore = store;
    }

    /**
     * Submits the configured assembly to Transloadit for processing.
     *
     * @param isResumable boolean value that tells the assembly whether or not to use tus.
     * @return {@link AssemblyResponse} the response received from the Transloadit server.
     * @throws RequestException        if request to Transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    public AssemblyResponse save(boolean isResumable)
            throws RequestException, LocalOperationException {
        Request request = new Request(getClient());
        options.put("steps", steps.toMap());

        AssemblyResponse response;
        // only do tus uploads if files will be uploaded
        if (isResumable && getFilesCount() > 0) {
            Map<String, String> tusOptions = new HashMap<String, String>();
            tusOptions.put("tus_num_expected_upload_files", Integer.toString(getFilesCount()));

            response = new AssemblyResponse(
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
        } else {
            response = new AssemblyResponse(request.post("/assemblies", options, null, files, fileStreams));
        }

        return shouldWaitForCompletion ? waitTillComplete(response) : response;
    }

    public AssemblyResponse save() throws LocalOperationException, RequestException {
        return this.save(true);
    }

    /**
     * If tus uploads are enabled, this method would be called by {@link Assembly#save()} to handle the file uploads.
     *
     * @param response {@link AssemblyResponse}
     * @throws IOException when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    protected void handleTusUpload(AssemblyResponse response) throws IOException, ProtocolException {
        processTusFiles(response.getSslUrl(), response.getTusUrl());
        uploadTusFiles();
    }

    /**
     * Prepares all files added for tus uploads.
     *
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @throws IOException       when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    protected void processTusFiles(String assemblyUrl, String tusUrl) throws IOException, ProtocolException {
        tusClient.setUploadCreationURL(new URL(tusUrl));
        tusClient.enableResuming(tusURLStore);

        for (Map.Entry<String, File> entry : files.entrySet()) {
            processTusFile(entry.getValue(), entry.getKey(), assemblyUrl);
        }

        for (Map.Entry<String, InputStream> entry : fileStreams.entrySet()) {
            processTusFile(entry.getValue(), entry.getKey(), assemblyUrl);
        }
    }

    /**
     * Prepares all files added for tus uploads.
     *
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @throws IOException       when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    @Deprecated
    protected void processTusFiles(String assemblyUrl) throws IOException, ProtocolException {
        processTusFiles(assemblyUrl, getClient().getHostUrl() + "/resumable/files/");
    }

    /**
     * Prepares a file for tus upload.
     *
     * @param inptStream {@link InputStream}
     * @param fieldName the form field name assigned to the file.
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @throws IOException when there's a failure with reading the input stream.
     */
    protected void processTusFile(InputStream inptStream, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(inptStream, fieldName, assemblyUrl);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", fieldName);
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }

    /**
     * Prepares a file for tus upload.
     *
     * @param file {@link File}
     * @param fieldName the form field name assigned to the file.
     * @param assemblyUrl the assembly url affiliated with the tus upload.
     * @throws IOException when there's a failure with file retrieval.
     */
    protected void processTusFile(File file, String fieldName, String assemblyUrl) throws IOException {
        TusUpload upload = getTusUploadInstance(file);

        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("filename", file.getName());
        metadata.put("assembly_url", assemblyUrl);
        metadata.put("fieldname", fieldName);

        upload.setMetadata(metadata);

        uploads.add(upload);
    }

    /**
     * Returns the {@link TusUpload} instance that would be used to upload a file.
     *
     * @param inputStream {@link InputStream}
     * @param fieldName {@link String} the field name assigned to the file
     * @param assemblyUrl {@link String} the assembly url
     * @return {@link TusUpload}
     * @throws IOException when there's a failure with reading the input stream.
     */
    protected TusUpload getTusUploadInstance(InputStream inputStream, String fieldName, String assemblyUrl) throws IOException {
        TusUpload tusUpload = new TusUpload();
        tusUpload.setInputStream(inputStream);
        tusUpload.setFingerprint(String.format("%s-%d-%s", fieldName, inputStream.available(), assemblyUrl));
        tusUpload.setSize(inputStream.available());

        return tusUpload;
    }

    /**
     * Returns the {@link TusUpload} instance that would be used to upload a file.
     *
     * @param file {@link File}
     * @return {@link TusUpload}
     * @throws FileNotFoundException when there's a failure with file retrieval.
     */
    protected TusUpload getTusUploadInstance(File file) throws FileNotFoundException {
        return new TusUpload(file);
    }

    /**
     * Does the actual uploading of files (when tus is enabled).
     *
     * @throws IOException when there's a failure with file retrieval.
     * @throws ProtocolException when there's a failure with tus upload.
     */
    protected void uploadTusFiles() throws IOException, ProtocolException {
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

    /**
     * Wait till the assembly is finished and then return the response of the complete state.
     *
     * @param response {@link AssemblyResponse}
     * @return {@link AssemblyResponse}
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     * @throws RequestException if request to Transloadit server fails.
     */
    protected AssemblyResponse waitTillComplete(AssemblyResponse response) throws LocalOperationException, RequestException {
        try {
            // wait for assembly to finish executing.
            while (!response.isFinished()) {
                Thread.sleep(1000);
                response = transloadit.getAssemblyByUrl(response.getSslUrl());
            }
        } catch (InterruptedException e) {
            throw new LocalOperationException(e);
        }

        return response;
    }
}

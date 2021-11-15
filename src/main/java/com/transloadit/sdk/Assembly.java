package com.transloadit.sdk;

import com.transloadit.sdk.exceptions.LocalOperationException;
import com.transloadit.sdk.exceptions.RequestException;
import com.transloadit.sdk.response.AssemblyResponse;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;
import io.tus.java.client.ProtocolException;
import io.tus.java.client.TusClient;
import io.tus.java.client.TusExecutor;
import io.tus.java.client.TusURLMemoryStore;
import io.tus.java.client.TusURLStore;
import io.tus.java.client.TusUpload;
import io.tus.java.client.TusUploader;
import org.jetbrains.annotations.TestOnly;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class represents a new assembly being created.
 */
public class Assembly extends OptionsBuilder {
    private TusURLStore tusURLStore;
    protected String assemblyId;

    protected Map<String, File> files;
    protected Map<String, InputStream> fileStreams;
    protected TusClient tusClient;
    protected List<TusUpload> uploads;
    protected boolean shouldWaitForCompletion;
    protected AssemblyListener assemblyListener;

    /**
     * Calls {@link #Assembly(Transloadit, Steps, Map, Map)} with the transloadit client as parameter.
     * @param transloadit {@link Transloadit} the transloadit client.
     */
    public Assembly(Transloadit transloadit) {
        this(transloadit, new Steps(), new HashMap<String, File>(), new HashMap<String, Object>());
    }

    /**
     * Constructs a new instance of the Assembly object.
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
        assemblyId = generateAssemblyID();

    }

    /**
     * Adds a file to your assembly. If the field name specified already exists, it will override the content of the
     * existing name. This also means that previously added and similarly named
     * {@link java.io.FileInputStream FileInputStreams} will be replaced.
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
     * existing name. This also means that previously added and similarly named
     * {@link java.io.File Files} will be replaced.
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
        if (files.containsKey(name)) {
            files.remove(name);
        }

        if (fileStreams.containsKey(name)) {
            fileStreams.remove(name);
        }
    }

    /**
     * Sets a listener that should be called after the assembly is done executing.
     *
     * @param assemblyListener {@link AssemblyListener}
     */
    public void setAssemblyListener(AssemblyListener assemblyListener) {
        this.assemblyListener = assemblyListener;
        shouldWaitForCompletion = assemblyListener != null;
    }

    /**
     * Returns the listener that should be called after assembly executing has been finished.
     * @return {@link AssemblyListener}
     */
    public AssemblyListener getAssemblyListener() {
        return assemblyListener;
    }

    /**
     * Determine whether or not to wait till the assembly is complete after it is saved.
     *
     * @deprecated use {@link #setAssemblyListener(AssemblyListener)} instead
     * @param shouldWaitForCompletion boolean value to determine whether or not to wait till the assembly is complete
     */
    @Deprecated
    public void setShouldWaitForCompletion(boolean shouldWaitForCompletion) {
        this.shouldWaitForCompletion = shouldWaitForCompletion;
    }

    /**
     * Normalizes a duplicated filename by adding an underscore and a incrementing number.
     * @param name duplicated Filename
     * @return renamed filename
     */
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
        if (!steps.toMap().isEmpty()) {
            options.put("steps", steps.toMap());
        }

        AssemblyResponse response;
        // only do tus uploads if files will be uploaded
        if (isResumable && getFilesCount() > 0) {
            Map<String, String> tusOptions = new HashMap<String, String>();
            tusOptions.put("tus_num_expected_upload_files", Integer.toString(getFilesCount()));

            response = new AssemblyResponse(
                    request.post(obtainUploadUrlSuffix(), options, tusOptions, null, null), true);

            // check if the assembly returned an error
            if (response.hasError()) {
                throw new RequestException("Request to Assembly failed: " + response.json().getString("error"));
            }

            if (shouldWaitWithSocket()) {
                listenToSocket(response);
            }

            try {
                handleTusUpload(response);
            } catch (IOException e) {
                throw new LocalOperationException(e);
            } catch (ProtocolException e) {
                throw new RequestException(e);
            }
        } else {
            response = new AssemblyResponse(request.post(obtainUploadUrlSuffix(), options, null, files, fileStreams));
            if (shouldWaitWithSocket() && !response.isFinished()) {
                listenToSocket(response);
            }
        }

        return shouldWaitWithoutSocket() ? waitTillComplete(response) : response;
    }

    /**
     * Calls {@link #save(boolean)} with boolean isResumable = true.
     * @return {@link AssemblyResponse} the response received from the Transloadit server.
     * @throws RequestException        if request to Transloadit server fails.
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
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
     * @param tusUrl the tus url affiliated with the tus upload.
     * @throws IOException       when there's a failure with file retrieval.
     */
    protected void processTusFiles(String assemblyUrl, String tusUrl) throws IOException {
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
     * @deprecated ideally this method should make uploads to the tus url assigned to an assembly, but it doesn't
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
    protected TusUpload getTusUploadInstance(InputStream inputStream, String fieldName, String assemblyUrl)
            throws IOException {
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
     * Determines if the Client should wait until the Assembly execution is finished by observing the
     * {@link AssemblyResponse} status. <p>Can only be {@code true} if <code>
     * {@link #shouldWaitForCompletion}  = true
     * </code> and no
     * {@link AssemblyListener} has been specified.</p>
     * @return <ul><li>{@code true} if the client should wait for Assembly completion by observing the
     * HTTP - Response;</li>
     * <li>{@code false} if the client should not wait for completion by observing the HTTP - Response</li></ul>
     * @see Assembly#save(boolean) Usage in Assembly.save()
     */
    protected boolean shouldWaitWithoutSocket() {
        return this.shouldWaitForCompletion && this.assemblyListener == null;
    }

    /**
     * Determines if the Client should wait until the Assembly execution is finished by observing a server socket. <p>
     * Can only be {@code true} if <code> {@link #shouldWaitForCompletion}  = true</code> and an
     * {@link AssemblyListener} has been specified.</p>
     * @return <ul><li>{@code true} if the client should wait for Assembly completion by observing the socket</li>
     * <li>{@code false} if the client should not wait for completion by observing the socket.</li></ul>
     * @see Assembly#save(boolean) Usage in Assembly.save()
     */
    protected boolean shouldWaitWithSocket() {
        return this.shouldWaitForCompletion && this.assemblyListener != null;
    }

    /**
     * Opens a Websocket to the provided URL in order to receive updates on the assembly's execution status.
     * @param socketUrl target url to open the WebSocket at.
     * @return {@link Socket}
     * @throws LocalOperationException
     */
    Socket getSocket(String socketUrl) throws LocalOperationException {
        IO.Options options = new IO.Options();
        options.transports = new String[] {WebSocket.NAME };
        try {
            URL url =  new URL(socketUrl);
            options.path = url.getPath();
            String host = url.getProtocol() + "://" + url.getHost();
            return IO.socket(host, options);
        } catch (URISyntaxException | MalformedURLException e) {
            throw new LocalOperationException(e);
        }
    }

    /**
     * Wait till the assembly is finished and then return the response of the complete state.
     *
     * @param response {@link AssemblyResponse}
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     */
    private void listenToSocket(AssemblyResponse response) throws LocalOperationException {
        final String assemblyUrl = response.getSslUrl();
        final String assemblyId = response.getId();

        final Socket socket = getSocket(response.getWebsocketUrl());
        Emitter.Listener onFinished = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.disconnect();
                try {
                    getAssemblyListener().onAssemblyFinished(transloadit.getAssemblyByUrl(assemblyUrl));
                } catch (RequestException e) {
                    getAssemblyListener().onError(e);
                } catch (LocalOperationException e) {
                    getAssemblyListener().onError(e);
                }
            }
        };

        Emitter.Listener onConnect = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject obj = new JSONObject();
                obj.put("id", assemblyId);
                socket.emit("assembly_connect", obj);
            }
        };

        Emitter.Listener onError = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                socket.disconnect();
                getAssemblyListener().onError((Exception) args[0]);
            }
        };

        Emitter.Listener onMetadataExtracted = args -> {
            getAssemblyListener().onMetadataExtracted();
        };

        Emitter.Listener onAssemblyResultFinished = args -> {
            String stepName = (String) args[0];
            JSONObject result = (JSONObject) args[1];
            getAssemblyListener().onAssemblyResultFinished(stepName, result);
        };

        //Hands over Filename of recently uploaded file to the callback in the AssemblyListener
        Emitter.Listener onFileUploadFinished = args -> {
            String name = ((JSONObject) args[0]).getString("name");
            JSONObject uploadInformation = (JSONObject) args[0];
            getAssemblyListener().onFileUploadFinished(name, uploadInformation);
        };

        // Triggers callback in the {@link Assembly#assemblyListener} if the Assembly instructions have been uploaded.
        Emitter.Listener onAssemblyUploadFinished = args -> {
                getAssemblyListener().onAssemblyUploadFinished();
        };

        socket
                .on(Socket.EVENT_CONNECT, onConnect)
                .on("assembly_finished", onFinished)
                .on("assembly_uploading_finished", onAssemblyUploadFinished)
                .on("assembly_upload_finished", onFileUploadFinished)
                .on("assembly_upload_meta_data_extracted", onMetadataExtracted)
                .on("assembly_result_finished", onAssemblyResultFinished)
                .on("assembly_error", onFinished)
                .on(Socket.EVENT_ERROR, onError);
        socket.connect();
    }

    /**
     * Wait till the assembly is finished and then return the response of the complete state.
     *
     * @param response {@link AssemblyResponse}
     * @return {@link AssemblyResponse}
     * @throws LocalOperationException if something goes wrong while running non-http operations.
     * @throws RequestException if request to Transloadit server fails.
     */
    protected AssemblyResponse waitTillComplete(AssemblyResponse response)
            throws LocalOperationException, RequestException {
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


    /**
     * Undocumented debug option, which is not intended for production use.
     * Providing custom Assembly IDs could lead to a security risk.
     * @param assemblyId custom Assembly ID
     * @throws LocalOperationException if the provided id doesn't match the expected pattern.
     */
    @TestOnly
    public void setAssemblyId(String assemblyId) throws LocalOperationException {
        String id = assemblyId.toLowerCase();
        if (id.matches("[a-f0-9]{32}")) { //Check ID Format
            this.assemblyId = id;
        } else {
            throw new LocalOperationException("The provided Assembly ID doesn't match the expected pattern of "
                    + "\"[a-f0-9]{32}\"");
        }
    }

    /**
     * Returns the assembly ID generated on client side to allow early logging. Be aware the Assembly ID will change if
     * you use the {@link Assembly#wipeAssemblyID()} method.
     * @return {@link String}AssemblyID
     */
    public String getClientSideGeneratedAssemblyID() {
        return assemblyId;
    }

    /**
     * Derives a new AssemblyID from an UUIDv4, without assigning it to the current assembly.
     * @return {@link String} Assembly ID
     */
    protected String generateAssemblyID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Wipes the client side generated Assembly-ID. As a result, the assembly id is assigned by the API after upload.
     * In this case you cannot obtain the Assembly ID before receiving a server response. As a result every Assembly ID
     * obtained by {@link Assembly#getClientSideGeneratedAssemblyID()} would be invalid.
     */
    protected void wipeAssemblyID() {
        this.assemblyId = "";
    }

    /**
     * Obtains the suffix of the upload url depending on if a custom or client side assembly ID has been set.
     * @return upload url suffix
     */
    protected String obtainUploadUrlSuffix() {
        if (assemblyId == null || assemblyId.isEmpty()) {
            return "/assemblies";
        } else {
            return "/assemblies/" + assemblyId;
        }
    }

}
